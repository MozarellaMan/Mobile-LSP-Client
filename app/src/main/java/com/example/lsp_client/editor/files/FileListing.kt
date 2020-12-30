package com.example.lsp_client.editor.files

import androidx.compose.material.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class FileNode(val path: String = "", val name: String = "", @SerialName("type") val fileType: String = "", val children: List<FileNode> = emptyList()) {
    fun isEmpty(): Boolean {
        return this.name.isBlank() && this.path.isBlank() && this.fileType.isBlank() && this.children.isEmpty()
    }
}

@Composable
fun DrawFileNode(fileNode: FileNode) {
    FileItem(fileName = fileNode.name, isDirectory = fileNode.fileType ==  "directory", onClick = {})
    if (fileNode.children.isNotEmpty()) {
        for (child in fileNode.children) {
            DrawFileNode(child)
        }
    }
}

@Composable
fun FileItem(fileName: String = "default", isDirectory: Boolean = false, onClick: () -> Unit) {
    val padding = 16.dp
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .padding(padding)
            .clickable(onClick = onClick)
            .fillMaxWidth()
    ) {
        Column {
            Row {
                if (isDirectory) {
                    Icon(imageVector = Icons.Filled.Menu, tint = Color.White)
                }
                    Text(text = fileName, color = Color.White)
            }
        }
    }
}