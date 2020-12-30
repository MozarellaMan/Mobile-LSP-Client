package com.example.lsp_client.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lsp_client.editor.files.DrawFileNode
import com.example.lsp_client.editor.files.FileNode

@Composable
fun FilePane(rootFileNode: FileNode) {
    Column (modifier = Modifier.padding(4.dp)) {
        DrawFileNode(rootFileNode)
    }
}