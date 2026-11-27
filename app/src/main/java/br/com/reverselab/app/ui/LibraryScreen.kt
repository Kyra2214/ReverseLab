package br.com.reverselab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    selectedArea: LibraryArea,
    onSelectArea: (LibraryArea) -> Unit,
    onBackToRoot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (selectedArea) {
        LibraryArea.ROOT -> LibraryRoot(onSelectArea, modifier)
        LibraryArea.PROJECTS -> LibraryAreaDetail(
            title = "Projetos",
            description = "Cada projeto preservará a amostra de origem, versões, arquivos relacionados, análises e relatórios.",
            emptyMessage = "Nenhum projeto foi importado ainda.",
            onBack = onBackToRoot,
            modifier = modifier,
        )
        LibraryArea.TECHNICAL -> LibraryAreaDetail(
            title = "Biblioteca Técnica",
            description = "Mecanismos, padrões, componentes e descobertas técnicas serão organizados por grupos e módulos.",
            emptyMessage = "Nenhuma descoberta técnica foi salva ainda.",
            onBack = onBackToRoot,
            modifier = modifier,
        )
        LibraryArea.VISUAL -> LibraryAreaDetail(
            title = "Biblioteca Visual",
            description = "Telas, menus, HUDs, ícones, botões, texturas, animações e referências visuais ficarão organizados aqui.",
            emptyMessage = "Nenhum ativo visual foi catalogado ainda.",
            onBack = onBackToRoot,
            modifier = modifier,
        )
    }
}

@Composable
private fun LibraryRoot(
    onSelectArea: (LibraryArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Biblioteca", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "O Knowledge Vault reúne a origem das investigações e o conhecimento reutilizável em três áreas conectadas.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LibraryAreaCard(
            title = "Projetos",
            description = "Amostras importadas, arquivos de origem, versões e histórico de investigação.",
            icon = Icons.Outlined.FolderOpen,
            onClick = { onSelectArea(LibraryArea.PROJECTS) },
        )
        LibraryAreaCard(
            title = "Biblioteca Técnica",
            description = "Arquitetura, código, mecanismos, dados, redes, segurança e descobertas.",
            icon = Icons.Outlined.Code,
            onClick = { onSelectArea(LibraryArea.TECHNICAL) },
        )
        LibraryAreaCard(
            title = "Biblioteca Visual",
            description = "Interfaces, componentes, imagens, texturas, animações e diagramas.",
            icon = Icons.Outlined.Image,
            onClick = { onSelectArea(LibraryArea.VISUAL) },
        )
    }
}

@Composable
private fun LibraryAreaCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LibraryAreaDetail(
    title: String,
    description: String,
    emptyMessage: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar à biblioteca")
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Biblioteca / $title", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emptyMessage, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Os registros aparecerão aqui conforme as amostras forem importadas e as descobertas forem salvas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onBack) { Text("Voltar à biblioteca") }
            }
        }
    }
}
