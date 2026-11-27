package br.com.reverselab.core.analysis

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads Android's compiled binary XML tree without executing or extracting the APK. */
class BinaryAndroidManifestParser {
    fun parse(bytes: ByteArray): AndroidManifestMetadata {
        if (bytes.size < 8) return AndroidManifestMetadata(parseStatus = "INVALID: arquivo menor que o cabeçalho XML.")
        return try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(u16(buffer, 0) == RES_XML_TYPE) { "Cabeçalho não é Android Binary XML." }
            val declaredSize = u32(buffer, 4).toInt()
            require(declaredSize in 8..bytes.size) { "Tamanho declarado do XML é inválido." }
            var offset = u16(buffer, 2)
            var strings: List<String> = emptyList()
            var packageName: String? = null
            var splitName: String? = null
            var configForSplit: String? = null
            val usesSplits = linkedSetOf<String>()
            while (offset + 8 <= declaredSize) {
                val type = u16(buffer, offset)
                val headerSize = u16(buffer, offset + 2)
                val chunkSize = u32(buffer, offset + 4).toInt()
                require(headerSize >= 8 && chunkSize >= headerSize && offset + chunkSize <= declaredSize) { "Chunk XML inválido." }
                when (type) {
                    RES_STRING_POOL_TYPE -> strings = readStringPool(buffer, offset, headerSize, chunkSize)
                    RES_XML_START_ELEMENT_TYPE -> {
                        val elementName = stringAt(strings, u32(buffer, offset + 20).toInt())
                        val attributeStart = u16(buffer, offset + 24)
                        val attributeSize = u16(buffer, offset + 26)
                        val attributeCount = u16(buffer, offset + 28)
                        require(attributeSize >= 20) { "Atributo XML com tamanho inválido." }
                        var attrOffset = offset + 16 + attributeStart
                        repeat(attributeCount) {
                            require(attrOffset + attributeSize <= offset + chunkSize) { "Atributo fora dos limites do chunk." }
                            val name = stringAt(strings, u32(buffer, attrOffset + 4).toInt())
                            val rawIndex = u32(buffer, attrOffset + 8).toInt()
                            val valueType = buffer.get(attrOffset + 15).toInt() and 0xff
                            val valueData = u32(buffer, attrOffset + 16).toInt()
                            val value = if (rawIndex >= 0) stringAt(strings, rawIndex) else typedValue(strings, valueType, valueData)
                            if (elementName == "manifest") {
                                when (name) {
                                    "package" -> packageName = value
                                    "split" -> splitName = value
                                    "configForSplit" -> configForSplit = value
                                }
                            }
                            if (elementName == "uses-split" && name == "name" && !value.isNullOrBlank()) usesSplits += value
                            attrOffset += attributeSize
                        }
                    }
                }
                offset += chunkSize
            }
            if (packageName.isNullOrBlank()) AndroidManifestMetadata(parseStatus = "INCOMPLETE: atributo package não encontrado.")
            else AndroidManifestMetadata(packageName, splitName, configForSplit, usesSplits.toList(), "PARSED")
        } catch (exception: Exception) {
            AndroidManifestMetadata(parseStatus = "INVALID: ${exception.message ?: "falha ao interpretar XML binário"}")
        }
    }

    private fun readStringPool(buffer: ByteBuffer, offset: Int, headerSize: Int, chunkSize: Int): List<String> {
        require(headerSize >= 28) { "Cabeçalho do string pool incompleto." }
        val stringCount = u32(buffer, offset + 8).toInt()
        val flags = u32(buffer, offset + 16).toInt()
        val stringsStart = u32(buffer, offset + 20).toInt()
        require(stringCount in 0..MAX_STRINGS && stringsStart in headerSize..chunkSize) { "String pool excede limites permitidos." }
        val utf8 = flags and UTF8_FLAG != 0
        return (0 until stringCount).map { index ->
            val relative = u32(buffer, offset + headerSize + index * 4).toInt()
            val position = offset + stringsStart + relative
            require(position in offset until offset + chunkSize) { "Offset de string inválido." }
            if (utf8) {
                val first = readLength8(buffer, position)
                val second = readLength8(buffer, first.second)
                val end = second.second + second.first
                require(end <= offset + chunkSize) { "String UTF-8 truncada." }
                val data = ByteArray(second.first)
                val duplicate = buffer.duplicate()
                duplicate.position(second.second)
                duplicate.get(data)
                data.toString(Charsets.UTF_8)
            } else {
                val first = readLength16(buffer, position)
                val byteLength = first.first * 2
                require(first.second + byteLength <= offset + chunkSize) { "String UTF-16 truncada." }
                val data = ByteArray(byteLength)
                val duplicate = buffer.duplicate()
                duplicate.position(first.second)
                duplicate.get(data)
                data.toString(Charsets.UTF_16LE)
            }
        }
    }

    private fun readLength8(buffer: ByteBuffer, position: Int): Pair<Int, Int> {
        val first = buffer.get(position).toInt() and 0xff
        return if (first and 0x80 == 0) first to position + 1 else (((first and 0x7f) shl 8) or (buffer.get(position + 1).toInt() and 0xff)) to position + 2
    }

    private fun readLength16(buffer: ByteBuffer, position: Int): Pair<Int, Int> {
        val first = u16(buffer, position)
        return if (first and 0x8000 == 0) first to position + 2 else (((first and 0x7fff) shl 16) or u16(buffer, position + 2)) to position + 4
    }

    private fun typedValue(strings: List<String>, type: Int, data: Int): String? = when (type) {
        TYPE_STRING -> stringAt(strings, data)
        TYPE_INT_DEC -> data.toString()
        TYPE_INT_HEX -> "0x${data.toUInt().toString(16)}"
        TYPE_INT_BOOLEAN -> (data != 0).toString()
        else -> null
    }

    private fun stringAt(strings: List<String>, index: Int): String? = if (index == NO_INDEX) null else strings.getOrNull(index)
    private fun u16(buffer: ByteBuffer, offset: Int) = buffer.getShort(offset).toInt() and 0xffff
    private fun u32(buffer: ByteBuffer, offset: Int) = buffer.getInt(offset).toLong() and 0xffffffffL

    private companion object {
        const val RES_XML_TYPE = 0x0003
        const val RES_STRING_POOL_TYPE = 0x0001
        const val RES_XML_START_ELEMENT_TYPE = 0x0102
        const val UTF8_FLAG = 0x00000100
        const val TYPE_STRING = 0x03
        const val TYPE_INT_DEC = 0x10
        const val TYPE_INT_HEX = 0x11
        const val TYPE_INT_BOOLEAN = 0x12
        const val NO_INDEX = -1
        const val MAX_STRINGS = 100_000
    }
}

data class AndroidManifestMetadata(
    val packageName: String? = null,
    val splitName: String? = null,
    val configForSplit: String? = null,
    val usesSplits: List<String> = emptyList(),
    val parseStatus: String,
)