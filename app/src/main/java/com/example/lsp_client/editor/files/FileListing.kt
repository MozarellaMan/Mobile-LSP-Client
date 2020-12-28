package com.example.lsp_client.editor.files

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                    Icon(Icons.Filled.Menu)
                }
                Spacer(Modifier.preferredSize(padding))
                Text(fileName)
            }
        }
    }
}