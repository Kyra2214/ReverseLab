package br.com.reverselab.core.analysis

import br.com.reverselab.core.domain.ArchiveEntryRecord
import br.com.reverselab.core.domain.ArchiveInspectionId
import br.com.reverselab.core.domain.ArchiveInspectionStatus
import br.com.reverselab.core.domain.ArchiveInventoryRecord
import br.com.reverselab.core.domain.SampleRecord
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.Enumeration
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

/** Inventories ZIP metadata only. It never extracts or executes archive entries. */
class ArchiveInspector {
    fun inspect(sample: SampleRecord, archiveFile: File, inspectedAtEpochMillis: Long): ArchiveInventoryRecord {
        require(inspectedAtEpochMillis > 0)
        if (!archiveFile.exists() || !archiveFile.isFile) {
            return emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.FAILED, "Arquivo privado da amostra não encontrado.")
        }
        if (archiveFile.length() > MAX_ARCHIVE_BYTES) {
            return emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.FAILED, "Contêiner excede o limite de inspeção de ${MAX_ARCHIVE_BYTES} bytes.")
        }
        if (sample.formatCode.uppercase(Locale.ROOT) !in CONTAINER_FORMATS) {
            return emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.NOT_ARCHIVE, "Formato declarado não é um contêiner ZIP suportado nesta etapa.")
        }

        return try {
            ZipFile(archiveFile).use { zip -> inspectZip(sample, zip, inspectedAtEpochMillis) }
        } catch (exception: ZipException) {
            emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.INVALID_ARCHIVE, "Estrutura ZIP inválida: ${exception.message ?: "erro de leitura"}")
        } catch (exception: IOException) {
            emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.FAILED, "Falha ao ler metadados do contêiner: ${exception.message ?: "erro de I/O"}")
        } catch (exception: SecurityException) {
            emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.FAILED, "Acesso negado ao arquivo privado da amostra.")
        } catch (exception: Exception) {
            emptyResult(sample, inspectedAtEpochMillis, ArchiveInspectionStatus.FAILED, "Falha inesperada no inventário do contêiner: ${exception.message ?: "erro interno"}")
        }
    }

    private fun inspectZip(sample: SampleRecord, zip: ZipFile, inspectedAtEpochMillis: Long): ArchiveInventoryRecord {
        val entries = mutableListOf<ArchiveEntryRecord>()
        val warnings = linkedSetOf<String>()
        fun addGlobalWarning(message: String) {
            if (warnings.size < MAX_WARNINGS) warnings += message
        }
        val seenNames = HashSet<String>()
        var entryCount = 0
        var totalNameCharacters = 0L
        var suspiciousEntryCount = 0
        var hasAndroidManifest = false
        var hasBaseManifest = false
        var dexEntryCount = 0
        var hasResourcesTable = false
        var hasNativeLibraries = false
        var hasXapkManifest = false
        var hasApksToc = false
        var hasBundleConfig = false
        var apkEntryCount = 0
        var obbEntryCount = 0
        var truncated = false
        var manifestMetadata: AndroidManifestMetadata? = null

        val enumeration: Enumeration<out ZipEntry> = zip.entries()
        while (enumeration.hasMoreElements()) {
            if (entryCount >= MAX_ENTRIES || totalNameCharacters >= MAX_TOTAL_NAME_CHARACTERS) {
                truncated = true
                addGlobalWarning("Inventário limitado por proteção de volume; existem entradas não listadas.")
                break
            }
            val entry = enumeration.nextElement()
            entryCount++
            val path = entry.name
            val displayPath = path.take(MAX_ENTRY_PATH_LENGTH)
            totalNameCharacters += path.length
            val normalizedPath = displayPath.replace('\\', '/').lowercase(Locale.ROOT)
            val entryWarnings = mutableListOf<String>()

            if (path.length > MAX_ENTRY_PATH_LENGTH) entryWarnings += "Nome de entrada excede o limite de exibição."
            if (isUnsafePath(displayPath)) entryWarnings += "Caminho de entrada suspeito."
            if (!seenNames.add(path)) entryWarnings += "Nome de entrada duplicado no contêiner."
            if (entry.size > MAX_ENTRY_UNCOMPRESSED_BYTES) entryWarnings += "Tamanho descompactado excede o limite de referência."
            if (entry.size > 0 && entry.compressedSize == 0L) entryWarnings += "Entrada sem tamanho comprimido declarado, mas com conteúdo descompactado."
            if (entry.size > 0 && entry.compressedSize > 0 && entry.size / entry.compressedSize.coerceAtLeast(1) > MAX_COMPRESSION_RATIO) {
                entryWarnings += "Taxa de compressão incomum; requer análise cuidadosa."
            }
            if (entryWarnings.isNotEmpty()) {
                suspiciousEntryCount++
                entryWarnings.forEach { addGlobalWarning("$displayPath: $it") }
            }

            if (entries.size < MAX_STORED_ENTRIES) {
                entries += ArchiveEntryRecord(
                    path = displayPath.ifBlank { "<entrada sem nome>" },
                    compressedBytes = entry.compressedSize,
                    uncompressedBytes = entry.size,
                    isDirectory = entry.isDirectory,
                    warnings = entryWarnings,
                )
            } else if (entries.size == MAX_STORED_ENTRIES && "A lista de entradas exibidas foi limitada a $MAX_STORED_ENTRIES; a contagem continua até o limite global." !in warnings) {
                addGlobalWarning("A lista de entradas exibidas foi limitada a $MAX_STORED_ENTRIES; a contagem continua até o limite global.")
            }

            if (normalizedPath == "androidmanifest.xml" || normalizedPath.endsWith("/manifest/androidmanifest.xml")) {
                hasAndroidManifest = true
                if (normalizedPath == "base/manifest/androidmanifest.xml") hasBaseManifest = true
            }
            if (DEX_ENTRY.matches(normalizedPath.substringAfterLast('/'))) dexEntryCount++
            if (normalizedPath == "resources.arsc" || normalizedPath.endsWith("/resources.arsc")) hasResourcesTable = true
            if (normalizedPath.startsWith("lib/") && normalizedPath.endsWith(".so")) hasNativeLibraries = true
            if (normalizedPath == "manifest.json") hasXapkManifest = true
            if (normalizedPath == "toc.pb") hasApksToc = true
            if (normalizedPath == "bundleconfig.pb") hasBundleConfig = true
            if (normalizedPath.endsWith(".apk")) apkEntryCount++
            if (normalizedPath.endsWith(".obb")) obbEntryCount++
        }

        val rootManifest = zip.getEntry("AndroidManifest.xml")
        if (rootManifest != null && rootManifest.size in 1..MAX_MANIFEST_BYTES) {
            manifestMetadata = zip.getInputStream(rootManifest).use { input ->
                val bytes = input.readBounded(MAX_MANIFEST_BYTES.toInt() + 1)
                if (bytes.size > MAX_MANIFEST_BYTES) AndroidManifestMetadata(parseStatus = "LIMIT_EXCEEDED: manifesto excede o limite de leitura.")
                else BinaryAndroidManifestParser().parse(bytes)
            }
            if (manifestMetadata?.parseStatus != "PARSED") addGlobalWarning("Manifesto Android não interpretado: ${manifestMetadata?.parseStatus ?: "resultado ausente"}")
        } else if (rootManifest != null) {
            manifestMetadata = AndroidManifestMetadata(parseStatus = "LIMIT_EXCEEDED: manifesto excede o limite de leitura.")
            addGlobalWarning("Manifesto Android excede o limite de leitura e não foi interpretado.")
        }

        val detectedType = when {
            hasApksToc && apkEntryCount > 0 -> "APKS"
            hasXapkManifest && apkEntryCount > 0 -> "XAPK"
            hasBundleConfig && hasBaseManifest -> "AAB"
            sample.formatCode.equals("OBB", ignoreCase = true) -> "OBB"
            hasAndroidManifest -> "APK"
            else -> "ZIP"
        }
        val declaredType = sample.formatCode.uppercase(Locale.ROOT)
        when (declaredType) {
            "APK" -> {
                if (!hasAndroidManifest) addGlobalWarning("APK não confirmado: AndroidManifest.xml não encontrado na raiz esperada do contêiner.")
                if (dexEntryCount == 0) addGlobalWarning("Nenhuma entrada classes*.dex encontrada; pode ser pacote sem código DEX ou arquivo incompatível.")
            }
            "XAPK" -> {
                if (!hasXapkManifest) addGlobalWarning("XAPK não confirmado: manifest.json não encontrado na raiz.")
                if (apkEntryCount == 0) addGlobalWarning("XAPK sem entradas APK internas identificáveis.")
            }
            "APKS" -> {
                if (!hasApksToc) addGlobalWarning("APKS não confirmado: toc.pb não encontrado na raiz.")
                if (apkEntryCount == 0) addGlobalWarning("APKS sem entradas APK internas identificáveis.")
            }
            "AAB" -> {
                if (!hasBundleConfig) addGlobalWarning("AAB não confirmado: BundleConfig.pb não encontrado.")
                if (!hasBaseManifest) addGlobalWarning("AAB sem base/manifest/AndroidManifest.xml.")
            }
            "OBB" -> if (entryCount == 0) addGlobalWarning("OBB sem entradas internas identificáveis.")
        }
        if (declaredType in PACKAGE_FORMATS && detectedType != declaredType) {
            addGlobalWarning("Formato declarado $declaredType diverge da estrutura interna preliminar ($detectedType).")
        }
        if (suspiciousEntryCount > 0) addGlobalWarning("$suspiciousEntryCount entrada(s) apresentam indicadores estruturais suspeitos; nenhuma foi extraída.")
        if (truncated) addGlobalWarning("Inventário parcial: limites de segurança impediram listar todo o contêiner.")

        return ArchiveInventoryRecord(
            id = ArchiveInspectionId(UUID.randomUUID().toString()),
            sampleId = sample.id,
            inspectedAtEpochMillis = inspectedAtEpochMillis,
            declaredFormat = declaredType,
            detectedContainerType = detectedType,
            status = if (truncated) ArchiveInspectionStatus.TRUNCATED else ArchiveInspectionStatus.INSPECTED,
            entryCount = entryCount,
            entries = entries,
            hasAndroidManifest = hasAndroidManifest,
            dexEntryCount = dexEntryCount,
            hasResourcesTable = hasResourcesTable,
            hasNativeLibraries = hasNativeLibraries,
            hasXapkManifest = hasXapkManifest,
            hasApksToc = hasApksToc,
            hasBundleConfig = hasBundleConfig,
            apkEntryCount = apkEntryCount,
            obbEntryCount = obbEntryCount,
            suspiciousEntryCount = suspiciousEntryCount,
            warnings = warnings.take(MAX_WARNINGS),
            truncated = truncated,
            manifestPackageName = manifestMetadata?.packageName,
            manifestSplitName = manifestMetadata?.splitName,
            manifestConfigForSplit = manifestMetadata?.configForSplit,
            manifestUsesSplits = manifestMetadata?.usesSplits ?: emptyList(),
            manifestParseStatus = manifestMetadata?.parseStatus,
        )
    }

    private fun emptyResult(
        sample: SampleRecord,
        inspectedAtEpochMillis: Long,
        status: ArchiveInspectionStatus,
        warning: String,
    ) = ArchiveInventoryRecord(
        id = ArchiveInspectionId(UUID.randomUUID().toString()),
        sampleId = sample.id,
        inspectedAtEpochMillis = inspectedAtEpochMillis,
        declaredFormat = sample.formatCode.uppercase(Locale.ROOT),
        detectedContainerType = null,
        status = status,
        entryCount = 0,
        entries = emptyList(),
        hasAndroidManifest = false,
        dexEntryCount = 0,
        hasResourcesTable = false,
        hasNativeLibraries = false,
        hasXapkManifest = false,
        hasApksToc = false,
        hasBundleConfig = false,
        apkEntryCount = 0,
        obbEntryCount = 0,
        suspiciousEntryCount = 0,
        warnings = listOf(warning),
        truncated = false,
    )

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (output.size() <= limit) {
            val remaining = limit + 1 - output.size()
            if (remaining <= 0) break
            val count = read(buffer, 0, minOf(buffer.size, remaining))
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun isUnsafePath(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        return normalized.startsWith('/') ||
            normalized.split('/').any { it == ".." } ||
            WINDOWS_DRIVE_PREFIX.containsMatchIn(normalized) ||
            path.contains('\u0000')
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L
        const val MAX_ENTRIES = 50_000
        const val MAX_STORED_ENTRIES = 300
        const val MAX_ENTRY_PATH_LENGTH = 4_096
        const val MAX_TOTAL_NAME_CHARACTERS = 2_000_000L
        const val MAX_ENTRY_UNCOMPRESSED_BYTES = 2L * 1024L * 1024L * 1024L
        const val MAX_COMPRESSION_RATIO = 1_000L
        const val MAX_WARNINGS = 100
        const val MAX_MANIFEST_BYTES = 2L * 1024L * 1024L
        val DEX_ENTRY = Regex("classes(\\d*)\\.dex")
        val WINDOWS_DRIVE_PREFIX = Regex("^[a-zA-Z]:")
        val CONTAINER_FORMATS = setOf("APK", "OBB", "XAPK", "APKS", "AAB", "ZIP")
        val PACKAGE_FORMATS = setOf("APK", "OBB", "XAPK", "APKS", "AAB")
    }
}
