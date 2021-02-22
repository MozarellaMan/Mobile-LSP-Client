package com.ayo.lsp_client.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayo.lsp_client.editor.EditorViewModel
import com.ayo.lsp_client.server.initializeLspWebSocket
import com.ayo.lsp_client.ui.editor.files.FilePane
import kotlinx.coroutines.flow.MutableSharedFlow
import lsp_proxy_tools.FileNode
import org.eclipse.lsp4j.Diagnostic

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Editor(ipAddress: String, rootUri: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val rootDirectory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    val currentFile: String by editorViewModel.currentFile.observeAsState("")
    val currentCodeOutput: String by editorViewModel.currentCodeOutput.observeAsState("")
    val diagnostics: List<Diagnostic> by editorViewModel.diagnostics.observeAsState(emptyList())
    val highestDiagnostic: Color by editorViewModel.highestDiagnosticSeverity.observeAsState(Color.White)
    val webSocketScope = rememberCoroutineScope()
    val listenerScope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Open)
    val messageFlow by remember { mutableStateOf(MutableSharedFlow<String>()) }
    val messageList = remember { mutableStateListOf<String>() }
    var sessionStarted by remember { mutableStateOf(false) }
    var diagnosticsVisible by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableStateOf(0) }

    if (!sessionStarted) {
        initializeLspWebSocket(
            onSessionStart = { sessionStarted = true },
            editorViewModel = editorViewModel,
            ipAddress = ipAddress,
            webSocketSendingScope = webSocketScope,
            webSocketListeningScope = listenerScope,
            rootUri = rootUri,
            messageFlow = messageFlow,
            messageOutputList = messageList
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                EditorAppBar(
                    ipAddress,
                    { diagnosticsVisible = !diagnosticsVisible },
                    editorViewModel,
                    highestDiagnostic,
                    drawerState,
                    { tabIndex = 0 },
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
                BottomDrawerContent(
                    drawerState,
                    currentCodeOutput,
                    messageList,
                    tabIndex,
                    editorViewModel
                ) {
                    tabIndex = it
                }
            }
        ) {
            MainEditorBody(diagnosticsVisible, diagnostics, editorViewModel, currentFile)
        }
    }
}

@Composable
private fun EditorAppBar(
    ipAddress: String,
    onInfoPressed: () -> Unit,
    editorViewModel: EditorViewModel,
    highestDiagnostic: Color,
    drawerState: BottomDrawerState,
    onRun: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ipAddress,
                textAlign = TextAlign.Center
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = {
                onInfoPressed()
                editorViewModel.refreshDiagnostics()
            }) {
                Icon(
                    Icons.Default.Info,
                    "Code Diagnostics",
                    tint = highestDiagnostic
                )
            }
            IconButton(onClick = {
                onRun()
                editorViewModel.getCodeOutput()
                drawerState.open()
            }) {
                Icon(Icons.Default.PlayArrow, "Run code")
            }
            IconButton(onClick = {
                editorViewModel.killRunningCode()
                drawerState.open()
            }) {
                Icon(Icons.Default.Clear, "Stop code")
            }
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
                activeColor = Color.Transparent,
                visualTransformation = { text ->
                    val lineIndexes = text.indices.filter { text[it] == '\n' || it == 0 }.map {
                        if (it == 0) {
                            0
                        } else {
                            it + 1
                        }
                    }
                    TransformedText(
                        AnnotatedString(
                            text = text.text,
                            spanStyles = diagnostics.map { diagnostic ->
                                AnnotatedString.Range(
                                    SpanStyle(
                                        color = editorViewModel.colorFromDiagnosticSeverity(
                                            diagnostic.severity
                                        ),
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    lineIndexes[diagnostic.range.start.line] + diagnostic.range.start.character,
                                    lineIndexes[diagnostic.range.end.line] + diagnostic.range.end.character
                                )
                            }
                        ), OffsetMapping.Identity)
                }
            )
        }
    }
}

@Composable
private fun BottomDrawerContent(
    drawerState: BottomDrawerState,
    currentCodeOutput: String,
    messageList: SnapshotStateList<String>,
    tabIndex: Int,
    editorViewModel: EditorViewModel,
    onTabIndexChange: (Int) -> Unit
) {
    val titles = listOf("Output", "Input")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.padding(8.dp)) {
            Button(
                onClick = { drawerState.close() },
                content = { Text("Close Drawer") })
        }

        TabRow(selectedTabIndex = tabIndex) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = tabIndex == index,
                    onClick = { onTabIndexChange(index) }
                )
            }
        }
        when (tabIndex) {
            0 -> {
                CodeOutputTab(currentCodeOutput = currentCodeOutput)
                DebugLspTab(messageList = messageList)
            }
            1 -> {
                CodeInputTab(editorViewModel)
            }
        }

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

@Composable
private fun CodeInputTab(editorViewModel: EditorViewModel) {
    val currentInput = editorViewModel.currentCodeInput.observeAsState("")
    Row {
        TextField(
            textStyle = TextStyle(color = Color.Black),
            value = currentInput.value,
            onValueChange = {
                editorViewModel.setCurrentInput(it)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}