package br.com.reverselab.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.reverselab.core.analysis.FileDetection
import br.com.reverselab.core.analysis.SafFormatInspector
import br.com.reverselab.core.domain.ImportBatchId
import br.com.reverselab.core.domain.SampleAssociationConfidence
import br.com.reverselab.core.domain.SampleCatalog
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.storage.ImportDocumentRequest
import br.com.reverselab.core.storage.SafPermissionManager
import br.com.reverselab.core.storage.SampleImportCoordinator
import java.util.UUID
import java.util.concurrent.Executors

private data class SelectedDocument(
    val uri: Uri,
    val displayName: String,
    val detection: FileDetection? = null,
    val isInspecting: Boolean = false,
    val selectedForImport: Boolean = false,
)

@Composable
fun AnalyzeScreen(
    sampleCatalog: SampleCatalog,
    importCoordinator: SampleImportCoordinator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionManager = remember(context.contentResolver) {
        SafPermissionManager(context.contentResolver)
    }
    val inspector = remember(context.contentResolver) { SafFormatInspector(context.contentResolver) }
    val inspectionExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(inspectionExecutor) {
        onDispose { inspectionExecutor.shutdown() }
    }
    val selectedDocuments = remember(permissionManager) {
        mutableStateListOf<SelectedDocument>().apply {
            permissionManager.listPersistedReadGrants().forEach { uri ->
                add(SelectedDocument(uri, resolveDisplayName(context.contentResolver, uri)))
            }
        }
    }
    val importedSamples = remember(sampleCatalog) {
        mutableStateListOf<SampleRecord>().apply { addAll(sampleCatalog.listAll()) }
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        statusMessage = null
        uris.forEach { uri ->
            try {
                check(permissionManager.persistReadGrant(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)) {
                    "O Android não manteve a permissão de leitura deste documento."
                }
                val name = resolveDisplayName(context.contentResolver, uri)
                val itemIndex = selectedDocuments.size
                selectedDocuments.add(
                    SelectedDocument(uri, name, isInspecting = true, selectedForImport = true),
                )
                inspectionExecutor.execute {
                    try {
                        val detection = inspector.inspect(uri, name)
                        mainHandler.post {
                            if (itemIndex in selectedDocuments.indices) {
                                selectedDocuments[itemIndex] = selectedDocuments[itemIndex].copy(
                                    detection = detection,
                                    isInspecting = false,
                                )
                            }
                        }
                    } catch (exception: Exception) {
                        mainHandler.post {
                            if (itemIndex in selectedDocuments.indices) {
                                selectedDocuments[itemIndex] = selectedDocuments[itemIndex].copy(isInspecting = false)
                            }
                            statusMessage = "O acesso foi salvo, mas não foi possível identificar o formato de $name."
                        }
                    }
                }
            } catch (exception: SecurityException) {
                statusMessage = "Não foi possível manter acesso de leitura a um dos arquivos selecionados."
            } catch (exception: IllegalArgumentException) {
                statusMessage = exception.message ?: "Permissão de arquivo inválida."
            } catch (exception: IllegalStateException) {
                statusMessage = exception.message ?: "Não foi possível confirmar a permissão persistente."
            }
        }
    }

    val pendingImportCount = selectedDocuments.count { it.selectedForImport }

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Analisar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Selecione arquivos pelo seletor seguro do Android. O ReverseLab identifica o tipo pelo cabeçalho e importa uma cópia de trabalho para a área privada, preservando o original.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = !isImporting,
            onClick = { documentPicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null)
            Text("Selecionar arquivos")
        }
        Button(
            enabled = pendingImportCount > 0 && !isImporting,
            onClick = {
                val pendingIndices = selectedDocuments.indices.filter { selectedDocuments[it].selectedForImport }
                val pendingDocuments = pendingIndices.map { selectedDocuments[it] }
                isImporting = true
                statusMessage = "Importação em andamento. Os originais não serão alterados."
                inspectionExecutor.execute {
                    try {
                        val requests = pendingDocuments.map { document ->
                            val detection = document.detection
                                ?: inspector.inspect(document.uri, document.displayName)
                            ImportDocumentRequest(
                                uri = document.uri,
                                displayName = document.displayName,
                                mimeType = detection.mimeType,
                                formatCode = detection.format.name,
                            )
                        }
                        val results = importCoordinator.importBatch(
                            batchId = ImportBatchId(UUID.randomUUID().toString()),
                            documents = requests,
                            importedAtEpochMillis = System.currentTimeMillis(),
                        )
                        val updatedCatalog = sampleCatalog.listAll()
                        val incompleteCount = results.count { it.errorMessage != null }
                        mainHandler.post {
                            importedSamples.clear()
                            importedSamples.addAll(updatedCatalog)
                            pendingIndices.forEach { index ->
                                if (index in selectedDocuments.indices) {
                                    selectedDocuments[index] = selectedDocuments[index].copy(selectedForImport = false)
                                }
                            }
                            isImporting = false
                            statusMessage = if (incompleteCount == 0) {
                                "${results.size} arquivo(s) importado(s). Hashes e associações foram registrados."
                            } else {
                                "$incompleteCount arquivo(s) tiveram importação parcial ou falha. Os artefatos parciais foram preservados."
                            }
                        }
                    } catch (exception: Exception) {
                        mainHandler.post {
                            isImporting = false
                            statusMessage = "A importação foi interrompida. Os arquivos já gravados e seus registros foram preservados."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.FolderCopy, contentDescription = null)
            Text(if (isImporting) "Importando…" else "Importar selecionados ($pendingImportCount)")
        }
        statusMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Arquivos com acesso concedido (${selectedDocuments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (selectedDocuments.isEmpty()) {
                item {
                    EmptyListCard("Nenhum arquivo selecionado. Use o seletor do Android para escolher uma ou mais amostras.")
                }
            } else {
                items(selectedDocuments) { document -> SelectedDocumentCard(document) }
            }
            if (importedSamples.isNotEmpty()) {
                item {
                    Text(
                        "Amostras importadas (${importedSamples.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(importedSamples) { sample -> ImportedSampleCard(sample) }
            }
        }
    }
}

@Composable
private fun SelectedDocumentCard(document: SelectedDocument) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(document.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                if (document.selectedForImport) "Selecionado para importação" else "Permissão de leitura persistente ativa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            val detection = document.detection
            Text(
                when {
                    document.isInspecting -> "Inspecionando cabeçalho do arquivo…"
                    detection != null -> "Formato: ${detection.format.label} • ${detection.confidence.label}"
                    else -> "Formato ainda não inspecionado nesta sessão"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportedSampleCard(sample: SampleRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(sample.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${sample.formatCode} • ${formatBytes(sample.sizeBytes)} • ${sample.status.name}", style = MaterialTheme.typography.bodySmall)
            sample.sha256?.let { hash -> Text("SHA-256: $hash", style = MaterialTheme.typography.labelSmall) }
            Text(
                "Grupo: ${sample.associationGroupId.value} • ${associationLabel(sample.associationConfidence)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sample.duplicateOfSampleId?.let { duplicate ->
                Text("Possível duplicata de ${duplicate.value}. Nenhum arquivo foi removido.", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyListCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun associationLabel(confidence: SampleAssociationConfidence): String = when (confidence) {
    SampleAssociationConfidence.IMPORT_BATCH_ONLY -> "mesmo lote de importação; pacote não confirmado"
    SampleAssociationConfidence.HEURISTIC_SINGLE_BASE_IN_BATCH -> "associação heurística; requer confirmação"
    SampleAssociationConfidence.AMBIGUOUS_BATCH -> "associação ambígua; revisão necessária"
    SampleAssociationConfidence.USER_CONFIRMED -> "associação confirmada pelo usuário"
    SampleAssociationConfidence.MANIFEST_VERIFIED -> "manifesto verificado"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}

private fun resolveDisplayName(
    contentResolver: android.content.ContentResolver,
    uri: Uri,
): String = try {
    val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
        ?: uri.lastPathSegment ?: "Documento sem nome"
} catch (_: Exception) {
    uri.lastPathSegment ?: "Documento sem nome"
}
