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
import com.example.lsp_client.editor.EditorViewModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class FileNode(val path: String = "", val name: String = "", @SerialName("type") val fileType: String = "", val children: List<FileNode> = emptyList(), @Transient var parent: FileNode? = null) {
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
fun DrawFileNode(root: FileNode,fileNode: FileNode, editorViewModel: EditorViewModel, onClick: () -> Unit) {
    FileItem(root,fileNode, editorViewModel, onClick)
    if (fileNode.children.isNotEmpty()) {
        for (child in fileNode.children) {
            child.parent = fileNode
            DrawFileNode(root,child, editorViewModel, onClick)
        }
    }
}

@Composable
fun FileItem(root: FileNode, fileNode: FileNode, editorViewModel: EditorViewModel, onClick: () -> Unit) {
    val padding = 16.dp
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .padding(padding)
            .clickable(onClick = {
                if (!fileNode.isDirectory()) {
                    editorViewModel.currentPath = fileNode.getPath(root)
                    editorViewModel.getCurrentFile()
                    onClick()
                }
            })
            .fillMaxWidth()
    ) {
        Column {
            Row {
                if (fileNode.isDirectory()) {
                    Icon(imageVector = Icons.Filled.Menu, "Directory burger menu", tint = Color.White)
                }
                    Text(text = fileNode.name, color = Color.White)
            }
        }
    }
}