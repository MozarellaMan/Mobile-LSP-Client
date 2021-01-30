package com.example.lsp_client.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import com.example.lsp_client.editor.files.FileNode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.compose.ui.Alignment
import com.example.lsp_client.ui.purple700
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import com.example.lsp_client.editor.files.FilePane
import com.example.lsp_client.server.*
import com.google.gson.Gson
import io.ktor.http.cio.websocket.*

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow


class EditorViewModel(var address: String = "") : ViewModel() {
    var opened: Boolean = false
    val directory = MutableLiveData<FileNode>()
    var currentPath: String = ""
    var outgoing: SendChannel<Frame> = Channel()
    var currentFile = MutableLiveData<String>()
    val gson = Gson()
    var initialized = false

    fun getCurrentFile() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentFile.value = getFile(address, currentPath)
        }
    }

    fun editCurrentFile(edits: String, languageMessageDispatch: LanguageMessageDispatch) {
        viewModelScope.launch {
            if (editFile(address,currentPath,edits)) {
                outgoing.send(Frame.Text(gson.toJson(languageMessageDispatch.textDidChange(currentPath, edits))))
                currentFile.value = edits
            }
        }
    }

    fun getFileDirectory() {
        if (address.isBlank()) return
        viewModelScope.launch {
            directory.value = getDirectory(address)
        }
    }
}

@Composable
fun Editor(ipAddress: String, rootUri: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val rootDirectory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    val currentFile: String by editorViewModel.currentFile.observeAsState("")
    val languageMessageDispatch = remember { LanguageMessageDispatch(rootUri) }
    val webSocketScope = rememberCoroutineScope()
    val listenerScope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Open)
    val messageFlow by remember { mutableStateOf(MutableSharedFlow<String>()) }
    val messageList = remember { mutableStateListOf<String>() }
    var sessionStarted by remember { mutableStateOf(false) }
    editorViewModel.address = ipAddress
    webSocketScope.launch {
        if (!sessionStarted) {
            val session = startSession(ipAddress)
            editorViewModel.outgoing = session.outgoing
            session.incoming.consumeAsFlow().collect {
                sessionStarted = true
                when (it) {
                    is Frame.Text -> {
                        val response = it.readText()
                        if (response.contains("language/status") && response.contains("Ready") && !editorViewModel.initialized) {
                            session.outgoing.send(Frame.Text(editorViewModel.gson.toJson(initialized)))
                            editorViewModel.initialized = true
                        }
                        messageFlow.emit(response)
                    }
                    else -> println("unrecognized msg")
                }
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
            TopAppBar(title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(ipAddress)
                    Spacer(Modifier.padding(16.dp))
                    Button(onClick = {
                        drawerState.open()
                        listenerScope.launch {
                            editorViewModel.outgoing.send(
                                Frame.Text(
                                    languageMessageDispatch.initialize(
                                        listOf("\"hoverProvider\" : \"true\"")
                                    )
                                )
                            )
                        }
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = purple700)) {
                        Text("Debug LSP")
                    }
                    //Button(onClick = {})
                }
            }, navigationIcon = {
                Icon(
                    Icons.Default.Menu,
                    "Burger menu icon",
                    modifier = Modifier.clickable(onClick = {
                        editorViewModel.getFileDirectory()
                        scaffoldState.drawerState.open()
                    })
                )
            })
        },
        drawerContent = {
            Text(text = "$ipAddress's files", modifier = Modifier.padding(16.dp))
            FilePane(rootFileNode = rootDirectory, editorViewModel, onClick = {
                listenerScope.launch {
                    if(!editorViewModel.opened) {
                        editorViewModel.opened = true
                        editorViewModel.outgoing.send(
                            Frame.Text(
                                languageMessageDispatch.textDocOpen(
                                    editorViewModel.currentPath,
                                    getFile(ipAddress, editorViewModel.currentPath)
                                )
                            )
                        )
                    }
                    editorViewModel.outgoing.send(
                        Frame.Text(
                            languageMessageDispatch.semanticTokenLegend().second
                        )
                    )
                }
            })
        },
        scaffoldState = scaffoldState,
        backgroundColor = Color.Black,
        drawerBackgroundColor = Color.Black
    ) {
        BottomDrawerLayout(
            drawerState = drawerState,
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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

            }
        ) {
            TextField(
                value = currentFile,
                onValueChange = { editorViewModel.editCurrentFile(it,languageMessageDispatch) },
                textStyle = TextStyle(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}