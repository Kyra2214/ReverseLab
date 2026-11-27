package br.com.reverselab.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private data class MainDestination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val destinations = listOf(
    MainDestination("Início", Icons.Outlined.Home),
    MainDestination("Biblioteca", Icons.Outlined.LibraryBooks),
    MainDestination("Analisar", Icons.Outlined.Science),
    MainDestination("IA", Icons.Outlined.SmartToy),
    MainDestination("Ajustes", Icons.Outlined.Settings),
)

@Composable
fun ReverseLabApp() {
    var selectedDestination by remember { mutableIntStateOf(0) }
    var selectedLibraryArea by remember { mutableStateOf(LibraryArea.ROOT) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = { selectedDestination = index },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedDestination) {
            0 -> HomeScreen(
                modifier = contentModifier,
                onOpenLibrary = { area ->
                    selectedLibraryArea = area
                    selectedDestination = 1
                },
                onOpenAnalysis = { selectedDestination = 2 },
            )
            1 -> LibraryScreen(
                modifier = contentModifier,
                selectedArea = selectedLibraryArea,
                onSelectArea = { selectedLibraryArea = it },
                onBackToRoot = { selectedLibraryArea = LibraryArea.ROOT },
            )
            2 -> FeaturePlaceholderScreen(
                modifier = contentModifier,
                title = "Analisar",
                description = "A área de análise reunirá a importação de amostras, a fila de processamento e os resultados das investigações.",
            )
            3 -> FeaturePlaceholderScreen(
                modifier = contentModifier,
                title = "IA",
                description = "Aqui serão configurados o modelo local e os provedores externos, com consentimento antes de qualquer envio.",
            )
            else -> FeaturePlaceholderScreen(
                modifier = contentModifier,
                title = "Ajustes",
                description = "Preferências do aplicativo, armazenamento, preservação de dados e configurações de segurança.",
            )
        }
    }
}
