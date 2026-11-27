package br.com.reverselab.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import java.io.File
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import br.com.reverselab.core.analysis.ArchiveInspector
import br.com.reverselab.core.analysis.FileDetection
import br.com.reverselab.core.analysis.SafFormatInspector
import br.com.reverselab.core.domain.ArchiveInventoryRecord
import br.com.reverselab.core.domain.ImportBatchId
import br.com.reverselab.core.domain.SampleAssociationConfidence
import br.com.reverselab.core.domain.SampleAssociationReviewAction
import br.com.reverselab.core.domain.SampleAssociationUpdate
import br.com.reverselab.core.domain.SampleCatalog
import br.com.reverselab.core.domain.SampleGroupId
import br.com.reverselab.core.domain.SampleRole
import br.com.reverselab.core.domain.SampleId
import br.com.reverselab.core.domain.SampleRecord
import br.com.reverselab.core.storage.ImportBatchSummary
import br.com.reverselab.core.storage.ImportHistoryRepository
import br.com.reverselab.core.storage.ImportHistorySnapshot
import br.com.reverselab.core.storage.ImportDocumentRequest
import br.com.reverselab.core.storage.SafPermissionManager
import br.com.reverselab.core.storage.SampleImportCoordinator
import br.com.reverselab.core.storage.StorageUsageSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val archiveInspector = remember { ArchiveInspector() }
    val historyRepository = remember(context, sampleCatalog) {
        ImportHistoryRepository(context, sampleCatalog)
    }
    val inspectionExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val historySnapshotState = remember { mutableStateOf<ImportHistorySnapshot?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var inspectingSampleId by remember { mutableStateOf<SampleId?>(null) }
    var isReviewingAssociation by remember { mutableStateOf(false) }
    DisposableEffect(inspectionExecutor) {
        onDispose { inspectionExecutor.shutdown() }
    }
    val refreshHistory = remember(historyRepository, inspectionExecutor, mainHandler) {
        {
            inspectionExecutor.execute {
                try {
                    val snapshot = historyRepository.loadSnapshot()
                    mainHandler.post { historySnapshotState.value = snapshot }
                } catch (exception: Exception) {
                    mainHandler.post { statusMessage = "Não foi possível atualizar o histórico de importação." }
                }
            }
        }
    }
    val submitAssociationReview = remember(sampleCatalog, historyRepository, inspectionExecutor, mainHandler) {
        { updates: List<SampleAssociationUpdate> ->
            if (updates.isNotEmpty() && !isReviewingAssociation) {
                isReviewingAssociation = true
                inspectionExecutor.execute {
                    try {
                        sampleCatalog.reviewAssociations(updates, System.currentTimeMillis())
                        val updatedSnapshot = try { historyRepository.loadSnapshot() } catch (_: Exception) { null }
                        mainHandler.post {
                            if (updatedSnapshot != null) historySnapshotState.value = updatedSnapshot
                            isReviewingAssociation = false
                            statusMessage = if (updatedSnapshot == null) {
                                "Revisão salva, mas o histórico não foi atualizado. Use Atualizar para tentar novamente."
                            } else {
                                "Revisão de associação registrada no histórico."
                            }
                        }
                    } catch (exception: Exception) {
                        mainHandler.post {
                            isReviewingAssociation = false
                            statusMessage = "Não foi possível salvar a revisão. Nenhuma exclusão ou alteração parcial foi aplicada."
                        }
                    }
                }
            }
        }
    }
    val selectedDocuments = remember(permissionManager) {
        mutableStateListOf<SelectedDocument>().apply {
            permissionManager.listPersistedReadGrants().forEach { uri ->
                add(SelectedDocument(uri, resolveDisplayName(context.contentResolver, uri)))
            }
        }
    }
    DisposableEffect(historyRepository) {
        refreshHistory()
        onDispose { }
    }

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
                selectedDocuments.add(SelectedDocument(uri, name, isInspecting = true, selectedForImport = true))
                inspectionExecutor.execute {
                    try {
                        val detection = inspector.inspect(uri, name)
                        mainHandler.post {
                            if (itemIndex in selectedDocuments.indices) {
                                selectedDocuments[itemIndex] = selectedDocuments[itemIndex].copy(
                                    detection = detection, isInspecting = false,
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

    Column(modifier = modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            val detection = document.detection ?: inspector.inspect(document.uri, document.displayName)
                            ImportDocumentRequest(document.uri, document.displayName, detection.mimeType, detection.format.name)
                        }
                        val results = importCoordinator.importBatch(
                            ImportBatchId(UUID.randomUUID().toString()), requests, System.currentTimeMillis(),
                        )
                        val snapshot = try { historyRepository.loadSnapshot() } catch (_: Exception) { null }
                        val incompleteCount = results.count { it.errorMessage != null }
                        mainHandler.post {
                            if (snapshot != null) historySnapshotState.value = snapshot
                            pendingIndices.forEach { index ->
                                if (index in selectedDocuments.indices) {
                                    selectedDocuments[index] = selectedDocuments[index].copy(selectedForImport = false)
                                }
                            }
                            isImporting = false
                            statusMessage = when {
                                snapshot == null -> "Importação concluída, mas o histórico não foi atualizado. Use Atualizar para tentar novamente."
                                incompleteCount > 0 -> "$incompleteCount arquivo(s) tiveram importação parcial ou falha. Os artefatos parciais foram preservados."
                                else -> "${results.size} arquivo(s) importado(s). Hashes e associações foram registrados."
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
        statusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Arquivos com acesso concedido (${selectedDocuments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (selectedDocuments.isEmpty()) {
                item { EmptyListCard("Nenhum arquivo selecionado. Use o seletor do Android para escolher uma ou mais amostras.") }
            } else {
                items(selectedDocuments) { document -> SelectedDocumentCard(document) }
            }
            val snapshot = historySnapshotState.value
            if (snapshot == null) {
                item { Text("Carregando histórico e armazenamento…", style = MaterialTheme.typography.bodySmall) }
            } else {
                item { StorageUsageCard(snapshot.storage) }
                item {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Histórico de importações (${snapshot.batches.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = refreshHistory) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Text("Atualizar")
                        }
                    }
                }
                if (snapshot.batches.isEmpty()) {
                    item { EmptyListCard("Nenhuma importação registrada ainda.") }
                } else {
                    items(snapshot.batches) { batch ->
                        ImportBatchCard(
                            batch = batch,
                            archiveInventories = snapshot.archiveInventories,
                            inspectingSampleId = inspectingSampleId,
                            isReviewingAssociation = isReviewingAssociation,
                            onConfirmGroup = { members ->
                                submitAssociationReview(
                                    members.map { member ->
                                        SampleAssociationUpdate(
                                            sampleId = member.id,
                                            newGroupId = member.associationGroupId,
                                            newConfidence = SampleAssociationConfidence.USER_CONFIRMED,
                                            action = SampleAssociationReviewAction.CONFIRM_GROUP,
                                        )
                                    },
                                )
                            },
                            onSeparateGroup = { members ->
                                submitAssociationReview(
                                    members.map { member ->
                                        SampleAssociationUpdate(
                                            sampleId = member.id,
                                            newGroupId = SampleGroupId("sample:${member.id.value}"),
                                            newConfidence = SampleAssociationConfidence.USER_REJECTED,
                                            action = SampleAssociationReviewAction.SEPARATE_GROUP,
                                        )
                                    },
                                )
                            },
                            onAssignSample = { sample, base ->
                                val confirmedGroupId = SampleGroupId("sample:${base.id.value}")
                                submitAssociationReview(
                                    listOf(
                                        SampleAssociationUpdate(sample.id, confirmedGroupId, SampleAssociationConfidence.USER_CONFIRMED, SampleAssociationReviewAction.ASSIGN_TO_BASE),
                                        SampleAssociationUpdate(base.id, confirmedGroupId, SampleAssociationConfidence.USER_CONFIRMED, SampleAssociationReviewAction.ASSIGN_TO_BASE),
                                    ),
                                )
                            },
                            onSeparateSample = { sample ->
                                submitAssociationReview(
                                    listOf(
                                        SampleAssociationUpdate(sample.id, SampleGroupId("sample:${sample.id.value}"), SampleAssociationConfidence.USER_REJECTED, SampleAssociationReviewAction.SEPARATE_GROUP),
                                    ),
                                )
                            },
                            onInspectSample = { sample ->
                                val relativePath = sample.internalRelativePath
                                if (sample.status.name != "IMPORTED" || relativePath == null) {
                                    statusMessage = "A amostra precisa estar importada integralmente antes da inspeção interna."
                                } else if (inspectingSampleId != null) {
                                    statusMessage = "Outra inspeção de contêiner já está em andamento."
                                } else {
                                    inspectingSampleId = sample.id
                                    statusMessage = "Inspecionando entradas do contêiner sem extrair arquivos…"
                                    inspectionExecutor.execute {
                                        try {
                                            val root = context.filesDir.canonicalFile
                                            val file = File(root, relativePath).canonicalFile
                                            require(file.path.startsWith(root.path + File.separator)) {
                                                "O caminho interno da amostra saiu da pasta privada autorizada."
                                            }
                                            val inventory = archiveInspector.inspect(sample, file, System.currentTimeMillis())
                                            sampleCatalog.saveArchiveInventory(inventory)
                                            val updatedSnapshot = historyRepository.loadSnapshot()
                                            mainHandler.post {
                                                historySnapshotState.value = updatedSnapshot
                                                inspectingSampleId = null
                                                statusMessage = "Inventário ${inventory.status.name}: ${inventory.entryCount} entrada(s), tipo interno ${inventory.detectedContainerType ?: "não confirmado"}."
                                            }
                                        } catch (exception: Exception) {
                                            mainHandler.post {
                                                inspectingSampleId = null
                                                statusMessage = "Não foi possível concluir o inventário interno. O arquivo original e a cópia de trabalho foram preservados."
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDocumentCard(document: SelectedDocument) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(document.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(if (document.selectedForImport) "Selecionado para importação" else "Permissão de leitura persistente ativa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
private fun StorageUsageCard(usage: StorageUsageSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Armazenamento privado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(formatBytes(usage.bytesOnDisk), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("${usage.fileCount} arquivo(s) • ${usage.cataloguedSamples} registro(s) no catálogo", style = MaterialTheme.typography.bodySmall)
            Text("Importados: ${usage.importedSamples} • Parciais: ${usage.partialSamples} • Pendentes: ${usage.pendingSamples} • Falhas: ${usage.failedSamples}", style = MaterialTheme.typography.bodySmall)
            Text("Possíveis duplicatas: ${usage.duplicateCandidates}. Nenhum arquivo é removido automaticamente.", style = MaterialTheme.typography.bodySmall)
            Text("Métrica referente à pasta privada de amostras, não ao espaço total do aplicativo.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImportBatchCard(
    batch: ImportBatchSummary,
    archiveInventories: Map<SampleId, ArchiveInventoryRecord>,
    inspectingSampleId: SampleId?,
    isReviewingAssociation: Boolean,
    onConfirmGroup: (List<SampleRecord>) -> Unit,
    onSeparateGroup: (List<SampleRecord>) -> Unit,
    onAssignSample: (SampleRecord, SampleRecord) -> Unit,
    onSeparateSample: (SampleRecord) -> Unit,
    onInspectSample: (SampleRecord) -> Unit,
) {
    var expanded by remember(batch.batchId) { mutableStateOf(false) }
    val baseCandidates = batch.samples.filter { it.role == SampleRole.BASE_APK_CANDIDATE && it.status.name == "IMPORTED" }
    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Lote ${batch.batchId.value.take(8)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(formatDate(batch.importedAtEpochMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${batch.totalSamples} arquivo(s) • ${formatBytes(batch.totalBytes)}", style = MaterialTheme.typography.bodyMedium)
            Text("Importados: ${batch.importedSamples} • Parciais: ${batch.partialSamples} • Pendentes: ${batch.pendingSamples} • Falhas: ${batch.failedSamples}", style = MaterialTheme.typography.bodySmall)
            Text("Formatos: ${batch.formats.joinToString()}", style = MaterialTheme.typography.bodySmall)
            if (batch.duplicateCandidates > 0) Text("Possíveis duplicatas: ${batch.duplicateCandidates}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            if (batch.associationReviews.isNotEmpty()) Text("Revisões de associação registradas: ${batch.associationReviews.size}", style = MaterialTheme.typography.labelSmall)
            if (baseCandidates.isEmpty() && batch.samples.any { it.role == SampleRole.SPLIT_APK_CANDIDATE || it.role == SampleRole.OBB_CANDIDATE }) {
                Text("Nenhum APK base importado com sucesso neste lote. O conjunto pode estar incompleto, mas isso ainda não pode ser confirmado sem os manifestos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(if (expanded) "Ocultar amostras" else "Ver amostras", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (expanded) {
                batch.samples.forEach { sample ->
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(sample.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("${sample.formatCode} • ${sample.status.name} • ${formatBytes(sample.sizeBytes)}", style = MaterialTheme.typography.labelSmall)
                        sample.sha256?.let { Text("SHA-256: $it", style = MaterialTheme.typography.labelSmall) }
                        Text(associationLabel(sample.associationConfidence), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (sample.role == SampleRole.BASE_APK_CANDIDATE && sample.associationConfidence == SampleAssociationConfidence.HEURISTIC_SINGLE_BASE_IN_BATCH) {
                            val groupMembers = batch.samples.filter { it.associationGroupId == sample.associationGroupId }
                            if (groupMembers.size > 1) {
                                AssociationReviewControls(groupMembers.size, !isReviewingAssociation, { onConfirmGroup(groupMembers) }, { onSeparateGroup(groupMembers) })
                            }
                        }
                        if (
                            (sample.role == SampleRole.SPLIT_APK_CANDIDATE || sample.role == SampleRole.OBB_CANDIDATE) &&
                            sample.associationConfidence in setOf(SampleAssociationConfidence.AMBIGUOUS_BATCH, SampleAssociationConfidence.USER_REJECTED, SampleAssociationConfidence.IMPORT_BATCH_ONLY) &&
                            baseCandidates.isNotEmpty()
                        ) {
                            AssociationAssignmentRow(
                                sample = sample,
                                baseCandidates = baseCandidates,
                                enabled = !isReviewingAssociation,
                                onAssign = { base -> onAssignSample(sample, base) },
                                onSeparate = { onSeparateSample(sample) },
                            )
                        }
                        if (sample.associationConfidence == SampleAssociationConfidence.USER_CONFIRMED) {
                            Text("Completude do conjunto ainda não verificada. A análise de manifesto será necessária.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        archiveInventories[sample.id]?.let { ArchiveInventorySummary(it) }
                        if (sample.formatCode.uppercase() in setOf("APK", "OBB", "XAPK", "APKS", "AAB", "ZIP") && sample.status.name == "IMPORTED") {
                            OutlinedButton(enabled = inspectingSampleId == null, onClick = { onInspectSample(sample) }) {
                                Text(if (inspectingSampleId == sample.id) "Inspecionando…" else "Inspecionar estrutura interna")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociationReviewControls(
    memberCount: Int,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onSeparate: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Associação automática candidata com $memberCount amostra(s). A completude do pacote permanece desconhecida.", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = enabled, onClick = onConfirm) { Text("Confirmar grupo") }
            OutlinedButton(enabled = enabled, onClick = onSeparate) { Text("Separar amostras") }
        }
    }
}

@Composable
private fun AssociationAssignmentRow(
    sample: SampleRecord,
    baseCandidates: List<SampleRecord>,
    enabled: Boolean,
    onAssign: (SampleRecord) -> Unit,
    onSeparate: () -> Unit,
) {
    var menuExpanded by remember(sample.id) { mutableStateOf(false) }
    var selectedBaseId by remember(sample.id) { mutableStateOf(baseCandidates.firstOrNull()?.id?.value.orEmpty()) }
    val selectedBase = baseCandidates.firstOrNull { it.id.value == selectedBaseId }
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Vínculo não confirmado. Escolha manualmente o APK base relacionado:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        OutlinedButton(enabled = enabled && baseCandidates.isNotEmpty(), onClick = { menuExpanded = true }) {
            Text(selectedBase?.displayName ?: "Nenhum APK base disponível")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            baseCandidates.forEach { base ->
                DropdownMenuItem(text = { Text(base.displayName) }, onClick = {
                    selectedBaseId = base.id.value
                    menuExpanded = false
                })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = enabled && selectedBase != null, onClick = { selectedBase?.let(onAssign) }) { Text("Vincular") }
            OutlinedButton(enabled = enabled, onClick = onSeparate) { Text("Separar") }
        }
        Text("A completude do conjunto continuará desconhecida até a análise dos manifestos.", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ArchiveInventorySummary(inventory: ArchiveInventoryRecord) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Estrutura: ${inventory.detectedContainerType ?: "não confirmada"} • ${inventory.status.name} • ${inventory.entryCount} entrada(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text("Manifesto: ${if (inventory.hasAndroidManifest) "sim" else "não"} • DEX: ${inventory.dexEntryCount} • APK internos: ${inventory.apkEntryCount} • OBB internos: ${inventory.obbEntryCount}", style = MaterialTheme.typography.labelSmall)
        Text("resources.arsc: ${if (inventory.hasResourcesTable) "sim" else "não"} • bibliotecas nativas: ${if (inventory.hasNativeLibraries) "sim" else "não"}", style = MaterialTheme.typography.labelSmall)
        inventory.warnings.take(4).forEach { warning ->
            Text("Atenção: $warning", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        }
        inventory.entries.take(8).forEach { entry ->
            Text("• ${entry.path}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyListCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun associationLabel(confidence: SampleAssociationConfidence): String = when (confidence) {
    SampleAssociationConfidence.IMPORT_BATCH_ONLY -> "Mesmo lote; pacote não confirmado"
    SampleAssociationConfidence.HEURISTIC_SINGLE_BASE_IN_BATCH -> "Associação heurística; requer confirmação"
    SampleAssociationConfidence.AMBIGUOUS_BATCH -> "Associação ambígua; revisão necessária"
    SampleAssociationConfidence.USER_CONFIRMED -> "Associação confirmada pelo usuário"
    SampleAssociationConfidence.USER_REJECTED -> "Associação separada pelo usuário"
    SampleAssociationConfidence.MANIFEST_VERIFIED -> "Manifesto verificado"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))

private fun resolveDisplayName(
    contentResolver: android.content.ContentResolver,
    uri: Uri,
): String = try {
    val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use { if (it.moveToFirst()) it.getString(0) else null } ?: uri.lastPathSegment ?: "Documento sem nome"
} catch (_: Exception) {
    uri.lastPathSegment ?: "Documento sem nome"
}
