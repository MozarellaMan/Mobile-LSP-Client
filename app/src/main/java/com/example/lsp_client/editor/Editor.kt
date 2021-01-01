package com.example.lsp_client.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.lsp_client.editor.files.FileNode
import com.example.lsp_client.server.LanguageServerSocket
import com.example.lsp_client.server.getDirectory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.compose.runtime.getValue

class EditorViewModel(var address: String = "", lspSocket: LanguageServerSocket? = null): ViewModel() {
    val directory: LiveData<FileNode> = liveData {
        val response = if (address.isBlank()) FileNode() else getDirectory(address)
        emit(response)
    }
}

@Composable
fun Editor(ipAddress: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val directory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    editorViewModel.address = ipAddress
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(ipAddress) }, navigationIcon = {
                Icon(
                    Icons.Default.Menu,
                    modifier = Modifier.clickable(onClick = {
                        scaffoldState.drawerState.open()
                    })
                )
            })
        },
        drawerContent = {
            Text(text = "$ipAddress's files", modifier = Modifier.padding(16.dp))
            FilePane(rootFileNode = directory)

        },
        scaffoldState = scaffoldState,
        backgroundColor = Color.Black,
        drawerBackgroundColor = Color.Black

    ) {
    }
}

