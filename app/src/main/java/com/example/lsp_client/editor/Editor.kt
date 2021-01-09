package com.example.lsp_client.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.lsp_client.editor.files.FileNode
import com.example.lsp_client.server.getDirectory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.compose.ui.Alignment
import com.example.lsp_client.server.startSession
import com.example.lsp_client.ui.purple700
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import com.example.lsp_client.server.LanguageServerSocket
import com.example.lsp_client.server.testInit
import io.ktor.http.cio.websocket.*
import io.ktor.utils.io.*

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow


class EditorViewModel(var address: String = ""): ViewModel() {
    val directory: LiveData<FileNode> = liveData {
        val response = if (address.isBlank()) FileNode() else getDirectory(address)
        emit(response)
    }

    var outgoing: SendChannel<Frame> = Channel()

}

@Composable
fun Editor(ipAddress: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val directory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    val webSocketScope = rememberCoroutineScope()
    val listenerScope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Open)
    val messageFlow by remember { mutableStateOf(MutableSharedFlow<String>()) }
    val messageList = remember { mutableStateListOf<String>() }
    editorViewModel.address = ipAddress
    webSocketScope.launch {
        val session  = startSession(ipAddress)
        editorViewModel.outgoing = session.outgoing
        session.incoming.consumeAsFlow().collect {
            when (it) {
                is Frame.Text -> messageFlow.emit(it.readText())
                else -> println("unrecognized msg")
            }
        }
    }
    listenerScope.launch {
        messageFlow.collect {
            messageList.add(it)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Row( verticalAlignment = Alignment.CenterVertically,modifier = Modifier.fillMaxWidth()) {
                Text(ipAddress)
                Spacer(Modifier.padding(16.dp))
                Button(onClick = {
                    drawerState.open()
                    listenerScope.launch {
                        editorViewModel.outgoing.send(Frame.Text(testInit))
                    } }, colors = ButtonDefaults.buttonColors(backgroundColor = purple700)) {
                    Text("Debug LSP")
                }
            } }, navigationIcon = {
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
        BottomDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                            Button(
                                    onClick = {
                                        drawerState.close()
                                    },
                                    content = { Text("Close Drawer") }
                            )
                        }
                        LazyColumn {
                            items(messageList.reversed()) {
                                Text(text = it, fontSize = 14.sp, color = Color.Black)
                            }
                        }
                    }

                },
                bodyContent = {}
        )
    }
}


