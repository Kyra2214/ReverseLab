package br.com.reverselab.core.analysis

import br.com.reverselab.core.domain.ApksTocApkRecord
import br.com.reverselab.core.domain.ApksTocModuleRecord
import br.com.reverselab.core.domain.ApksTocRecord
import br.com.reverselab.core.domain.ApksTocVariantRecord
import java.nio.charset.StandardCharsets

/**
 * Bounded, dependency-free reader for the public Bundletool BuildApksResult protobuf wire format.
 * Unknown fields are skipped by wire type; no APK payload is opened or executed here.
 */
class ApksTocParser {
    fun parse(bytes: ByteArray): ApksTocRecord {
        if (bytes.isEmpty()) return ApksTocRecord(null, emptyList(), "EMPTY")
        if (bytes.size > MAX_TOC_BYTES) return ApksTocRecord(null, emptyList(), "LIMIT_EXCEEDED")
        return runCatching {
            val root = ProtoReader(bytes).readFields()
            val packageName = root.firstBytes(4)?.utf8()?.takeIf { it.isNotBlank() }
            val variantFields = root.filter { it.number == 1 && it.bytes != null }
            val variants = variantFields.mapIndexed { index, field ->
                parseVariant(field.bytes!!, index)
            }
            if (variants.isEmpty()) {
                ApksTocRecord(packageName, emptyList(), "NO_VARIANTS")
            } else {
                ApksTocRecord(packageName, variants, if (packageName == null) "PARSED_WITHOUT_PACKAGE" else "PARSED")
            }
        }.getOrElse { error ->
            ApksTocRecord(null, emptyList(), "INVALID_PROTOBUF: ${error.javaClass.simpleName}")
        }
    }

    private fun parseVariant(bytes: ByteArray, fallbackNumber: Int): ApksTocVariantRecord {
        val fields = ProtoReader(bytes).readFields()
        val variantNumber = fields.firstVarint(3)?.toInt()?.coerceAtLeast(0) ?: fallbackNumber
        val moduleRecords = mutableListOf<ApksTocModuleRecord>()
        val apkRecords = mutableListOf<ApksTocApkRecord>()
        fields.filter { it.number == 2 && it.bytes != null }.forEach { setField ->
            val setFields = ProtoReader(setField.bytes!!).readFields()
            val module = setFields.firstBytes(1)?.let(::parseModule)
            if (module != null) moduleRecords += module
            setFields.filter { it.number == 2 && it.bytes != null }.forEach { apkField ->
                parseApkDescription(apkField.bytes!!, module?.name)?.let(apkRecords::add)
            }
        }
        return ApksTocVariantRecord(
            variantNumber = variantNumber,
            modules = moduleRecords,
            apks = apkRecords,
        )
    }

    private fun parseModule(bytes: ByteArray): ApksTocModuleRecord? {
        val fields = ProtoReader(bytes).readFields()
        val name = fields.firstBytes(1)?.utf8()?.takeIf { it.isNotBlank() } ?: return null
        val dependencies = fields.filter { it.number == 4 && it.bytes != null }
            .map { it.bytes!!.utf8() }
            .filter { it.isNotBlank() }
            .distinct()
        return ApksTocModuleRecord(name, dependencies)
    }

    private fun parseApkDescription(bytes: ByteArray, moduleName: String?): ApksTocApkRecord? {
        val fields = ProtoReader(bytes).readFields()
        val path = fields.firstBytes(2)?.utf8()?.replace('\\', '/')?.takeIf(::isSafePath) ?: return null
        val targetingBytes = fields.firstBytes(1)
        val targetingSummary = targetingBytes?.let(::parseTargeting).orEmpty()
        val splitMetadata = fields.firstBytes(3) ?: fields.firstBytes(5)
        val splitFields = splitMetadata?.let { ProtoReader(it).readFields() }
        val splitId = splitFields?.firstBytes(1)?.utf8()?.takeIf { it.isNotBlank() }
        val isMasterSplit = splitFields?.firstVarint(2)?.let { it != 0L }
        val metadataKind = when {
            fields.any { it.number == 3 && it.bytes != null } -> "SPLIT"
            fields.any { it.number == 5 && it.bytes != null } -> "INSTANT_SPLIT"
            fields.any { it.number == 4 && it.bytes != null } -> "STANDALONE"
            fields.any { it.number == 6 && it.bytes != null } -> "SYSTEM"
            fields.any { it.number == 8 && it.bytes != null } -> "APEX"
            fields.any { it.number == 9 && it.bytes != null } -> "ARCHIVED"
            else -> "UNKNOWN"
        }
        return ApksTocApkRecord(
            path = path,
            moduleName = moduleName,
            splitId = splitId,
            isMasterSplit = isMasterSplit,
            metadataKind = metadataKind,
            hasTargeting = fields.any { it.number == 1 && it.bytes != null },
            targetingSummary = targetingSummary,
        )
    }

    private fun parseTargeting(bytes: ByteArray): List<String> {
        val fields = ProtoReader(bytes).readFields()
        val summary = mutableListOf<String>()
        fields.firstBytes(1)?.let { abiBytes ->
            val abiFields = ProtoReader(abiBytes).readFields()
            val values = abiFields.filter { it.number == 1 && it.bytes != null }.mapNotNull { parseAbi(it.bytes!!) }
            val alternatives = abiFields.filter { it.number == 2 && it.bytes != null }.mapNotNull { parseAbi(it.bytes!!) }
            if (values.isNotEmpty() || alternatives.isNotEmpty()) {
                summary += "ABI value=${values.joinToString()} alternatives=${alternatives.joinToString()}"
            }
        }
        fields.firstBytes(3)?.let { languageBytes ->
            val languageFields = ProtoReader(languageBytes).readFields()
            val values = languageFields.filter { it.number == 1 && it.bytes != null }.map { it.bytes!!.utf8() }
            val alternatives = languageFields.filter { it.number == 2 && it.bytes != null }.map { it.bytes!!.utf8() }
            if (values.isNotEmpty() || alternatives.isNotEmpty()) {
                summary += "Idioma value=${values.joinToString()} alternatives=${alternatives.joinToString()}"
            }
        }
        fields.firstBytes(4)?.let { densityBytes ->
            val densityFields = ProtoReader(densityBytes).readFields()
            val values = densityFields.filter { it.number == 1 && it.bytes != null }.mapNotNull { parseDensity(it.bytes!!) }
            val alternatives = densityFields.filter { it.number == 2 && it.bytes != null }.mapNotNull { parseDensity(it.bytes!!) }
            if (values.isNotEmpty() || alternatives.isNotEmpty()) {
                summary += "Densidade value=${values.joinToString()} alternatives=${alternatives.joinToString()}"
            }
        }
        fields.firstBytes(5)?.let { sdkBytes ->
            val sdkFields = ProtoReader(sdkBytes).readFields()
            val values = sdkFields.filter { it.number == 1 && it.bytes != null }.mapNotNull { parseSdkMinimum(it.bytes!!) }
            val alternatives = sdkFields.filter { it.number == 2 && it.bytes != null }.mapNotNull { parseSdkMinimum(it.bytes!!) }
            if (values.isNotEmpty() || alternatives.isNotEmpty()) {
                summary += "SDK mínimo value=${values.joinToString()} alternatives=${alternatives.joinToString()}"
            }
        }
        fields.firstBytes(7)?.let { multiAbiBytes ->
            val multiAbiFields = ProtoReader(multiAbiBytes).readFields()
            val values = multiAbiFields.filter { it.number == 1 && it.bytes != null }.map { multiField ->
                val multi = ProtoReader(multiField.bytes!!).readFields()
                multi.filter { it.number == 1 && it.bytes != null }.mapNotNull { parseAbi(it.bytes!!) }.joinToString("+")
            }.filter { it.isNotBlank() }
            if (values.isNotEmpty()) summary += "Multi-ABI value=${values.joinToString()}"
        }
        return summary
    }

    private fun parseAbi(bytes: ByteArray): String? {
        val value = ProtoReader(bytes).readFields().firstVarint(1)?.toInt() ?: return null
        return when (value) {
            1 -> "armeabi"
            2 -> "armeabi-v7a"
            3 -> "arm64-v8a"
            4 -> "x86"
            5 -> "x86_64"
            6 -> "mips"
            7 -> "mips64"
            8 -> "riscv64"
            else -> null
        }
    }

    private fun parseDensity(bytes: ByteArray): String? {
        val fields = ProtoReader(bytes).readFields()
        fields.firstVarint(2)?.let { return "${it}dpi" }
        val alias = fields.firstVarint(1)?.toInt() ?: return null
        return when (alias) {
            1 -> "nodpi"
            2 -> "ldpi"
            3 -> "mdpi"
            4 -> "tvdpi"
            5 -> "hdpi"
            6 -> "xhdpi"
            7 -> "xxhdpi"
            8 -> "xxxhdpi"
            else -> null
        }
    }

    private fun parseSdkMinimum(bytes: ByteArray): String? {
        val sdkFields = ProtoReader(bytes).readFields()
        val wrapper = sdkFields.firstBytes(1) ?: return null
        return ProtoReader(wrapper).readFields().firstVarint(1)?.toString()
    }

    private fun isSafePath(path: String): Boolean {
        if (path.isBlank() || path.length > MAX_PATH_LENGTH || path.startsWith('/') || path.startsWith('\\')) return false
        if (Regex("^[a-zA-Z]:").containsMatchIn(path)) return false
        return path.split('/').none { it == ".." || it.isEmpty() }
    }

    private fun List<ProtoField>.firstBytes(number: Int): ByteArray? =
        firstOrNull { it.number == number && it.bytes != null }?.bytes

    private fun List<ProtoField>.firstVarint(number: Int): Long? =
        firstOrNull { it.number == number && it.varint != null }?.varint

    private fun ByteArray.utf8(): String = String(this, StandardCharsets.UTF_8)

    private data class ProtoField(
        val number: Int,
        val wireType: Int,
        val varint: Long? = null,
        val bytes: ByteArray? = null,
    )

    private class ProtoReader(private val input: ByteArray) {
        private var position = 0

        fun readFields(): List<ProtoField> {
            val fields = mutableListOf<ProtoField>()
            while (position < input.size) {
                val tag = readVarint()
                val fieldNumber = (tag ushr 3).toInt()
                val wireType = (tag and 7L).toInt()
                require(fieldNumber > 0) { "Invalid protobuf field number" }
                val field = when (wireType) {
                    0 -> ProtoField(fieldNumber, wireType, varint = readVarint())
                    1 -> {
                        skip(8)
                        ProtoField(fieldNumber, wireType)
                    }
                    2 -> {
                        val length = readVarint()
                        require(length in 0..MAX_TOC_BYTES.toLong()) { "Invalid protobuf field length" }
                        val size = length.toInt()
                        require(size <= input.size - position) { "Truncated protobuf field" }
                        val value = input.copyOfRange(position, position + size)
                        position += size
                        ProtoField(fieldNumber, wireType, bytes = value)
                    }
                    5 -> {
                        skip(4)
                        ProtoField(fieldNumber, wireType)
                    }
                    else -> throw IllegalArgumentException("Unsupported protobuf wire type $wireType")
                }
                fields += field
                require(fields.size <= MAX_PROTO_FIELDS) { "Too many protobuf fields" }
            }
            return fields
        }

        private fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (shift < 64) {
                require(position < input.size) { "Truncated protobuf varint" }
                val value = input[position++].toInt() and 0xff
                if (shift == 63) require((value and 0xfe) == 0) { "Protobuf varint overflows 64 bits" }
                result = result or ((value and 0x7f).toLong() shl shift)
                if ((value and 0x80) == 0) return result
                shift += 7
            }
            throw IllegalArgumentException("Protobuf varint is too long")
        }

        private fun skip(count: Int) {
            require(count >= 0 && count <= input.size - position) { "Truncated protobuf fixed-width field" }
            position += count
        }
    }

    private companion object {
        const val MAX_TOC_BYTES = 4 * 1024 * 1024
        const val MAX_PROTO_FIELDS = 100_000
        const val MAX_PATH_LENGTH = 4_096
    }
}
