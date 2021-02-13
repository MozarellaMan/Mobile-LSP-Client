package com.ayo.lsp_client.ui.editor.files

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayo.lsp_client.editor.EditorViewModel
import com.ayo.lsp_client.ui.theming.purple500

@Composable
fun FilePane(rootFileNode: FileNode, editorViewModel: EditorViewModel, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(4.dp)) {
        DrawFileNode(rootFileNode, rootFileNode, editorViewModel, onClick)
    }
}

@Composable
fun DrawFileNode(
    root: FileNode,
    fileNode: FileNode,
    editorViewModel: EditorViewModel,
    onClick: () -> Unit
) {
    var childrenVisible by remember { mutableStateOf(false) }
    FileItem(root, fileNode, editorViewModel, onClick, { childrenVisible = !childrenVisible })
    if (childrenVisible || !fileNode.isDirectory()) {
        if (fileNode.children.isNotEmpty()) {
            for (child in fileNode.children) {
                child.parent = fileNode
                DrawFileNode(root, child, editorViewModel, onClick)
            }
        }
    }
}

@Composable
fun FileItem(
    root: FileNode,
    fileNode: FileNode,
    editorViewModel: EditorViewModel,
    onClick: () -> Unit,
    onOpen: () -> Unit
) {
    val padding = 8.dp
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
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
//                    if(!fileNode.isRoot()) {
//                        Spacer(Modifier.padding(4.dp))
//                    }
                    var newFileDialog by remember { mutableStateOf(false) }
                    val dismissDialog = { newFileDialog = false }
                    Column(horizontalAlignment = Alignment.Start) {
                        IconButton(onClick = { onOpen() }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                "Directory burger menu",
                                tint = Color.White
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fileNode.name,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        IconButton(onClick = { newFileDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                "Create new file",
                                tint = Color.White
                            )
                        }
                    }
                    if (newFileDialog) {
                        NewFileDialog(dismissDialog, editorViewModel, fileNode, root)
                    }
                } else {
                    Text(text = fileNode.name, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    editorViewModel: EditorViewModel,
    fileNode: FileNode,
    rootFileNode: FileNode
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        backgroundColor = Color.Black,
        onDismissRequest = { onDismiss() },
        text = {
            TextField(
                modifier = Modifier.border(2.dp, purple500, RoundedCornerShape(4.dp)),
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "File Name") },
                textStyle = TextStyle(Color.White),
                activeColor = Color.Transparent,
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        editorViewModel.createNewFile(
                            directoryPath = fileNode.getPath(rootFileNode),
                            fileName = name
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
