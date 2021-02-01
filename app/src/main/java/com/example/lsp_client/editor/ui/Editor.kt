package com.example.lsp_client.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.viewModel
import com.example.lsp_client.editor.EditorViewModel
import com.example.lsp_client.editor.files.FileNode
import com.example.lsp_client.server.LanguageMessageDispatch
import com.example.lsp_client.server.startLanguageServerSession
import com.example.lsp_client.ui.purple700
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@Composable
fun Editor(ipAddress: String, rootUri: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val rootDirectory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    val currentFile: String by editorViewModel.currentFile.observeAsState("")
    val currentCodeOutput: String by editorViewModel.currentCodeOutput.observeAsState("")
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
            val session = startLanguageServerSession(ipAddress)
            editorViewModel.outgoingSocket = session.outgoing
            editorViewModel.languageMessageDispatch = languageMessageDispatch
            session.incoming.consumeAsFlow().collect {
                sessionStarted = true
                when (it) {
                    is Frame.Text -> {
                        val response = it.readText()
                        if (response.contains("language/status") && response.contains("Ready") && !editorViewModel.initialized) {
                            session.outgoing.send(Frame.Text(editorViewModel.gson.toJson(languageMessageDispatch.initialized)))
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
                            editorViewModel.outgoingSocket.send(
                                Frame.Text(
                                    languageMessageDispatch.initialize(
                                        listOf("\"hoverProvider\" : \"true\"")
                                    )
                                )
                            )
                        }
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = purple700)) {
                        Text("Init LSP")
                    }
                    Spacer(Modifier.padding(2.dp))
                    Button(onClick = {
                        editorViewModel.getCodeOutput()
                        drawerState.open()
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = purple700)) {
                        Icon(Icons.Default.PlayArrow, "Run code")
                    }
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
            FilePane(
                rootFileNode = rootDirectory,
                editorViewModel,
                onClick = { editorViewModel.getFile() })
        },
        scaffoldState = scaffoldState,
        backgroundColor = Color.Black,
        drawerBackgroundColor = Color.Black
    ) {
        BottomDrawerLayout(
            drawerState = drawerState,
            drawerContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Button( onClick = { drawerState.close() }, content = { Text("Close Drawer") })
                    }
                    if (currentCodeOutput.isNotBlank()){
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Surface(color = Color.DarkGray, modifier = Modifier.fillMaxWidth()) {
                                Text(modifier = Modifier.padding(8.dp), color = Color.White, text = currentCodeOutput)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyColumn {
                            items(messageList.reversed()) {
                                Text(text = it, fontSize = 14.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        ) {
            TextField(
                value = currentFile,
                onValueChange = { editorViewModel.editCurrentFile(it) },
                textStyle = TextStyle(Color.White),
                modifier = Modifier.fillMaxWidth(),
                activeColor = Color.Transparent
            )
        }
    }
}