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
    fun isDirectory(): Boolean {
        return this.fileType == "directory"
    }
    fun isEmpty(): Boolean {
        return this.name.isBlank() && this.path.isBlank() && this.fileType.isBlank() && this.children.isEmpty()
    }

    fun getPath(root: FileNode): String {
        return this.path.removePrefix("${root.path}/")
    }
}

@Composable
fun DrawFileNode(fileNode: FileNode, onClick: () -> Unit) {
    FileItem(fileNode, onClick)
    if (fileNode.children.isNotEmpty()) {
        for (child in fileNode.children) {
            DrawFileNode(child, onClick)
        }
    }
}

@Composable
fun FileItem(fileNode: FileNode, onClick: () -> Unit) {
    val padding = 16.dp
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .padding(padding)
            .clickable(onClick = onClick)
            .fillMaxWidth()
    ) {
        Column {
            Row {
                if (fileNode.isDirectory()) {
                    Icon(imageVector = Icons.Filled.Menu, tint = Color.White)
                }
                    Text(text = fileNode.name, color = Color.White)
            }
        }
    }
}