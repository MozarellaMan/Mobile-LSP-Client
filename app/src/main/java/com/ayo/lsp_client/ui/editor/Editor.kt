package com.ayo.lsp_client.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.viewModel
import com.ayo.lsp_client.editor.EditorViewModel
import com.ayo.lsp_client.server.LanguageMessageDispatch
import com.ayo.lsp_client.server.initializeLspWebSocket
import com.ayo.lsp_client.ui.editor.files.FileNode
import com.ayo.lsp_client.ui.editor.files.FilePane
import com.ayo.lsp_client.ui.theming.purple700
import kotlinx.coroutines.flow.MutableSharedFlow
import org.eclipse.lsp4j.Diagnostic

@ExperimentalAnimationApi
@Composable
fun Editor(ipAddress: String, rootUri: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val rootDirectory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    val currentFile: String by editorViewModel.currentFile.observeAsState("")
    val currentCodeOutput: String by editorViewModel.currentCodeOutput.observeAsState("")
    val diagnostics: List<Diagnostic> by editorViewModel.diagnostics.observeAsState(emptyList())
    val highestDiagnostic: Color by editorViewModel.highestDiagnosticSeverity.observeAsState(Color.White)
    val languageMessageDispatch = remember { LanguageMessageDispatch(rootUri) }
    val webSocketScope = rememberCoroutineScope()
    val listenerScope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Open)
    val messageFlow by remember { mutableStateOf(MutableSharedFlow<String>()) }
    val messageList = remember { mutableStateListOf<String>() }
    var sessionStarted by remember { mutableStateOf(false) }
    val diagnosticsVisible by remember { mutableStateOf(false) }
    if (!sessionStarted) {
        initializeLspWebSocket(
            onSessionStart = { sessionStarted = true },
            editorViewModel = editorViewModel,
            ipAddress = ipAddress,
            webSocketSendingScope = webSocketScope,
            webSocketListeningScope = listenerScope,
            languageMessageDispatch = languageMessageDispatch,
            messageFlow = messageFlow,
            messageOutputList = messageList
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                EditorAppBar(
                    ipAddress,
                    diagnosticsVisible,
                    editorViewModel,
                    highestDiagnostic,
                    drawerState,
                )
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
                BottomDrawerContent(drawerState, currentCodeOutput, messageList)
            }
        ) {
            MainEditorBody(diagnosticsVisible, diagnostics, editorViewModel, currentFile)
        }
    }
}

@Composable
private fun EditorAppBar(
    ipAddress: String,
    diagnosticsVisible: Boolean,
    editorViewModel: EditorViewModel,
    highestDiagnostic: Color,
    drawerState: BottomDrawerState,
) {
    var diagnosticsVisible1 = diagnosticsVisible
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(ipAddress)
        Spacer(Modifier.padding(8.dp))
        IconButton(onClick = {
            diagnosticsVisible1 = !diagnosticsVisible1
            editorViewModel.refreshDiagnostics()
        }) {
            Icon(
                Icons.Default.Info,
                "Code Diagnostics",
                tint = highestDiagnostic
            )
        }
        Button(
            onClick = {
                if (drawerState.isClosed) {
                    drawerState.open()
                } else {
                    drawerState.close()
                }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = purple700)
        ) {
            Text("Debug")
        }
        Spacer(Modifier.padding(2.dp))
        Button(onClick = {
            editorViewModel.getCodeOutput()
            drawerState.open()
        }, colors = ButtonDefaults.buttonColors(backgroundColor = purple700)) {
            Icon(Icons.Default.PlayArrow, "Run code")
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun MainEditorBody(
    diagnosticsVisible: Boolean,
    diagnostics: List<Diagnostic>,
    editorViewModel: EditorViewModel,
    currentFile: String
) {
    Column {
        AnimatedVisibility(visible = diagnosticsVisible && diagnostics.isNotEmpty()) {
            Row {
                Column(Modifier.padding(8.dp)) {
                    diagnostics.forEach {
                        Text(
                            text = it.message,
                            fontSize = 14.sp,
                            color = editorViewModel.colorFromDiagnosticSeverity(it.severity),
                        )
                    }
                }
            }
        }
        Row {
            TextField(
                value = currentFile,
                onValueChange = { editorViewModel.editCurrentFile(it) },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                activeColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun BottomDrawerContent(
    drawerState: BottomDrawerState,
    currentCodeOutput: String,
    messageList: SnapshotStateList<String>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.padding(8.dp)) {
            Button(
                onClick = { drawerState.close() },
                content = { Text("Close Drawer") })
        }
        CodeOutputTab(currentCodeOutput)
        DebugLspTab(messageList)
    }
}

@Composable
private fun DebugLspTab(messageList: SnapshotStateList<String>) {
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

@Composable
private fun CodeOutputTab(currentCodeOutput: String) {
    if (currentCodeOutput.isNotBlank()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Surface(color = Color.DarkGray, modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    color = Color.White,
                    text = currentCodeOutput
                )
            }
        }
    }
}