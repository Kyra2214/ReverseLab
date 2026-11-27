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
import br.com.reverselab.core.storage.SafPermissionManager
import java.util.concurrent.Executors

private data class SelectedDocument(
    val uri: Uri,
    val displayName: String,
    val detection: FileDetection? = null,
    val isInspecting: Boolean = false,
)

@Composable
fun AnalyzeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissionManager = remember(context.contentResolver) {
        SafPermissionManager(context.contentResolver)
    }
    val inspector = remember(context.contentResolver) { SafFormatInspector(context.contentResolver) }
    val inspectionExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(inspectionExecutor) {
        onDispose { inspectionExecutor.shutdownNow() }
    }
    val selectedDocuments = remember(permissionManager) {
        mutableStateListOf<SelectedDocument>().apply {
            permissionManager.listPersistedReadGrants().forEach { uri ->
                add(SelectedDocument(uri, resolveDisplayName(context.contentResolver, uri)))
            }
        }
    }
    var selectionError by remember { mutableStateOf<String?>(null) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        selectionError = null
        uris.forEach { uri ->
            try {
                permissionManager.persistReadGrant(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name = resolveDisplayName(context.contentResolver, uri)
                val itemIndex = selectedDocuments.size
                selectedDocuments.add(SelectedDocument(uri, name, isInspecting = true))
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
                            selectionError = "Não foi possível identificar o formato de $name. O acesso de leitura foi mantido."
                        }
                    }
                }
            } catch (exception: SecurityException) {
                selectionError = "Não foi possível manter acesso de leitura a um dos arquivos selecionados."
            } catch (exception: IllegalArgumentException) {
                selectionError = exception.message ?: "Permissão de arquivo inválida."
            }
        }
    }

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Analisar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Selecione arquivos pelo seletor seguro do Android. O ReverseLab mantém permissão de leitura e identifica o tipo pelo cabeçalho, sem copiar, extrair ou executar o conteúdo.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { documentPicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null)
            Text("Selecionar arquivos")
        }
        selectionError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Arquivos selecionados (${selectedDocuments.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (selectedDocuments.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Nenhum arquivo selecionado. Use o seletor do Android para escolher uma ou mais amostras.",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(selectedDocuments) { document ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(document.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "Permissão de leitura persistente ativa",
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
                            detection?.evidence?.forEach { evidence ->
                                Text(evidence, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveDisplayName(
    contentResolver: android.content.ContentResolver,
    uri: Uri,
): String {
    val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: uri.lastPathSegment ?: "Documento sem nome"
}
