package com.example.lsp_client.editor.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lsp_client.editor.EditorViewModel

@Composable
fun FilePane(rootFileNode: FileNode, editorViewModel: EditorViewModel, onClick: () -> Unit) {
    Column (modifier = Modifier.padding(4.dp)) {
        DrawFileNode(rootFileNode,rootFileNode, editorViewModel, onClick)
    }
}