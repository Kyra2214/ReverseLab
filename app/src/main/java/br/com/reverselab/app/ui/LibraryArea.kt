package br.com.reverselab.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.graphics.vector.ImageVector

enum class LibraryArea(
    val title: String,
    val description: String,
    val icon: ImageVector,
) {
    ROOT("Biblioteca", "Conhecimento organizado do ReverseLab", Icons.Outlined.FolderOpen),
    PROJECTS("Projetos", "Amostras importadas e investigações", Icons.Outlined.FolderOpen),
    TECHNICAL("Biblioteca Técnica", "Mecanismos, padrões e descobertas", Icons.Outlined.Code),
    VISUAL("Biblioteca Visual", "Telas, componentes e referências visuais", Icons.Outlined.Image),
}
