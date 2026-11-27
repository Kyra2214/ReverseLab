package br.com.reverselab.core.analysis

import java.util.Locale

enum class DetectedFormat(val label: String, val isContainer: Boolean = false) {
    APK("APK", true), OBB("OBB", true), XAPK("XAPK", true), APKS("APKS", true),
    AAB("Android App Bundle", true), ZIP("ZIP", true), DEX("DEX"),
    ELF("ELF / biblioteca nativa"), IMAGE("Imagem"), AUDIO("Áudio"),
    VIDEO("Vídeo"), PDF("PDF"), UNKNOWN("Desconhecido"),
}

enum class DetectionConfidence(val label: String) {
    SIGNATURE("Assinatura binária"),
    SIGNATURE_AND_EXTENSION("Assinatura do contêiner + extensão"),
    MIME_HINT("MIME informado pelo provedor"),
    UNKNOWN("Não identificado"),
}

data class FileDetection(
    val format: DetectedFormat,
    val confidence: DetectionConfidence,
    val displayName: String,
    val mimeType: String?,
    val evidence: List<String>,
)

/** Detects formats from a short header; it never extracts or executes file content. */
object FileTypeDetector {
    fun detect(displayName: String, mimeType: String?, header: ByteArray): FileDetection {
        val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val normalizedMime = mimeType?.lowercase(Locale.ROOT)
        val zipSignature = header.startsWithBytes(0x50, 0x4B) && header.size >= 4 &&
            (header[2].toInt() and 0xFF) in setOf(0x03, 0x05, 0x07) &&
            (header[3].toInt() and 0xFF) in setOf(0x04, 0x06, 0x08)

        if (zipSignature) {
            val format = when (extension) {
                "apk" -> DetectedFormat.APK
                "obb" -> DetectedFormat.OBB
                "xapk" -> DetectedFormat.XAPK
                "apks" -> DetectedFormat.APKS
                "aab" -> DetectedFormat.AAB
                else -> DetectedFormat.ZIP
            }
            val confidence = if (format == DetectedFormat.ZIP) {
                DetectionConfidence.SIGNATURE
            } else {
                DetectionConfidence.SIGNATURE_AND_EXTENSION
            }
            return result(
                format, confidence, displayName, mimeType,
                "Cabeçalho ZIP válido",
                if (format == DetectedFormat.ZIP) "Contêiner ZIP genérico" else "Extensão .$extension; conteúdo interno será inspecionado na importação",
            )
        }

        if (header.startsWithBytes(0x64, 0x65, 0x78, 0x0A)) {
            return result(DetectedFormat.DEX, DetectionConfidence.SIGNATURE, displayName, mimeType, "Magic DEX dex\\n")
        }
        if (header.startsWithBytes(0x7F, 0x45, 0x4C, 0x46)) {
            return result(DetectedFormat.ELF, DetectionConfidence.SIGNATURE, displayName, mimeType, "Magic ELF")
        }
        if (header.startsWithBytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) ||
            header.startsWithBytes(0xFF, 0xD8, 0xFF) ||
            header.startsWithAscii("GIF87a") || header.startsWithAscii("GIF89a") ||
            header.size >= 12 && header.startsWithAscii("RIFF") && header.asciiAt(8, 4) == "WEBP"
        ) {
            return result(DetectedFormat.IMAGE, DetectionConfidence.SIGNATURE, displayName, mimeType, "Assinatura de imagem reconhecida")
        }
        if (header.startsWithAscii("%PDF-")) {
            return result(DetectedFormat.PDF, DetectionConfidence.SIGNATURE, displayName, mimeType, "Magic PDF")
        }
        if ((header.size >= 8 && header.asciiAt(4, 4) == "ftyp") ||
            header.startsWithBytes(0x1A, 0x45, 0xDF, 0xA3)
        ) {
            return result(DetectedFormat.VIDEO, DetectionConfidence.SIGNATURE, displayName, mimeType, "Assinatura de contêiner de vídeo")
        }
        if (header.startsWithAscii("ID3") || header.startsWithAscii("fLaC") ||
            header.startsWithAscii("OggS") ||
            header.size >= 12 && header.startsWithAscii("RIFF") && header.asciiAt(8, 4) == "WAVE" ||
            header.size >= 2 && (header[0].toInt() and 0xFF) == 0xFF && (header[1].toInt() and 0xE0) == 0xE0
        ) {
            return result(DetectedFormat.AUDIO, DetectionConfidence.SIGNATURE, displayName, mimeType, "Assinatura de áudio reconhecida")
        }

        val mimeFormat = when {
            normalizedMime?.startsWith("image/") == true -> DetectedFormat.IMAGE
            normalizedMime?.startsWith("audio/") == true -> DetectedFormat.AUDIO
            normalizedMime?.startsWith("video/") == true -> DetectedFormat.VIDEO
            normalizedMime == "application/pdf" -> DetectedFormat.PDF
            else -> null
        }
        if (mimeFormat != null) {
            return result(mimeFormat, DetectionConfidence.MIME_HINT, displayName, mimeType, "MIME informado pelo provedor: $mimeType")
        }

        val extensionHint = when (extension) {
            "apk" -> "APK"
            "obb" -> "OBB"
            "xapk" -> "XAPK"
            "apks" -> "APKS"
            "aab" -> "AAB"
            "dex" -> "DEX"
            "so" -> "ELF"
            "zip" -> "ZIP"
            else -> null
        }
        val evidence = if (extensionHint == null) {
            listOf("Nenhuma assinatura reconhecida nos primeiros ${header.size} bytes")
        } else {
            listOf("Extensão sugere $extensionHint, mas a assinatura esperada não foi encontrada")
        }
        return FileDetection(DetectedFormat.UNKNOWN, DetectionConfidence.UNKNOWN, displayName, mimeType, evidence)
    }

    private fun result(
        format: DetectedFormat,
        confidence: DetectionConfidence,
        name: String,
        mime: String?,
        vararg evidence: String,
    ) = FileDetection(format, confidence, name, mime, evidence.toList())

    private fun ByteArray.startsWithBytes(vararg values: Int): Boolean =
        size >= values.size && values.indices.all { (this[it].toInt() and 0xFF) == values[it] }

    private fun ByteArray.startsWithAscii(value: String): Boolean =
        size >= value.length && asciiAt(0, value.length) == value

    private fun ByteArray.asciiAt(offset: Int, length: Int): String =
        copyOfRange(offset, offset + length).toString(Charsets.US_ASCII)
}
