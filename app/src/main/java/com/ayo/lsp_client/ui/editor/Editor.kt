package com.ayo.lsp_client.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch
import lsp_proxy_tools.FileNode
import org.eclipse.lsp4j.Diagnostic

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Editor(ipAddress: String, rootUri: String, editorViewModel: EditorViewModel = viewModel()) {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val rootDirectory: FileNode by editorViewModel.directory.observeAsState(FileNode())
    val currentFile: String by editorViewModel.currentFile.observeAsState("")
    val diagnostics: List<Diagnostic> by editorViewModel.diagnostics.observeAsState(emptyList())
    val highestDiagnostic: Color by editorViewModel.highestDiagnosticSeverity.observeAsState(Color.White)
    val webSocketScope = rememberCoroutineScope()
    val listenerScope = rememberCoroutineScope()
    val drawerScope = rememberCoroutineScope()
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Open)
    val messageFlow by remember { mutableStateOf(MutableSharedFlow<String>()) }
    val messageList = remember { mutableStateListOf<String>() }
    val codeOutputList = remember { mutableStateListOf<String>() }
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
                    {
                        codeOutputList.clear()
                        tabIndex = 0
                    },
                    codeOutputList,
                )
            }, navigationIcon = {
                Icon(
                    Icons.Default.Menu,
                    "Burger menu icon",
                    modifier = Modifier.clickable(onClick = {
                        editorViewModel.getFileDirectory()
                        drawerScope.launch {
                            scaffoldState.drawerState.open()
                            editorViewModel.refreshDiagnostics()
                        }
                    })
                )
            })
        },
        drawerContent = {
            Text(text = "$ipAddress's files", modifier = Modifier.padding(16.dp))
            FilePane(
                rootFileNode = rootDirectory,
                editorViewModel,
                onClick = {
                    editorViewModel.getFile()
                    diagnosticsVisible = false
                    diagnosticsVisible = true
                })
        },
        scaffoldState = scaffoldState,
        backgroundColor = Color.Black,
        drawerBackgroundColor = Color.Black
    ) {
        BottomDrawer(
            drawerState = drawerState,
            drawerContent = {
                BottomDrawerContent(
                    drawerState,
                    codeOutputList,
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
    codeOutputList: SnapshotStateList<String>
) {
    val drawerScope = rememberCoroutineScope()
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
                editorViewModel.runUserCode(codeOutputList)
                drawerScope.launch {
                    drawerState.open()
                }
            }) {
                Icon(Icons.Default.PlayArrow, "Run code")
            }
            IconButton(onClick = {
                editorViewModel.killRunningCode()
                drawerScope.launch {
                    drawerState.open()
                }
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
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                            spanStyles = if (text.isNotEmpty()) {
                                diagnostics
                                    .filter { diagnostic ->
                                        editorViewModel.diagnosticInFileRange(
                                            diagnostic,
                                            lineIndexes
                                        )
                                    }
                                    .map { diagnostic ->
                                        AnnotatedString.Range(
                                            SpanStyle(
                                                color = if (editorViewModel.colorFromDiagnosticSeverity(
                                                        diagnostic.severity
                                                    ) == Color.Yellow
                                                ) {
                                                    Color.White.copy(alpha = 0.8f)
                                                } else {
                                                    editorViewModel.colorFromDiagnosticSeverity(
                                                        diagnostic.severity
                                                    )
                                                },
                                                textDecoration = TextDecoration.Underline
                                            ),
                                            lineIndexes.getOrElse(diagnostic.range.start.line) { 0 } + diagnostic.range.start.character,
                                            lineIndexes.getOrElse(diagnostic.range.end.line) { 0 } + diagnostic.range.end.character
                                        )
                                    }
                            } else {
                                emptyList()
                            }
                        ), OffsetMapping.Identity)
                },
            )
        }
    }
}

@Composable
private fun BottomDrawerContent(
    drawerState: BottomDrawerState,
    codeOutputList: SnapshotStateList<String>,
    messageList: SnapshotStateList<String>,
    tabIndex: Int,
    editorViewModel: EditorViewModel,
    onTabIndexChange: (Int) -> Unit
) {
    val drawerScope = rememberCoroutineScope()
    val titles = listOf("Output", "LSP")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.padding(8.dp)) {
            Button(
                onClick = { drawerScope.launch { drawerState.close() } },
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
                CodeOutputTab(codeOutputList, editorViewModel)
            }
            1 -> {
                DebugLspTab(messageList = messageList)
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
private fun CodeOutputTab(
    codeOutputList: SnapshotStateList<String>,
    editorViewModel: EditorViewModel
) {
    var input by remember { mutableStateOf("") }
    val isCodeLoading: Boolean by editorViewModel.isCodeLoading.observeAsState(false)
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 1.dp, max = 150.dp)
                .background(Color.DarkGray)
        ) {
            if (isCodeLoading) {
                codeOutputList.clear()
                CircularProgressIndicator(modifier = Modifier.scale(0.7F))
                codeOutputList.clear()
            }
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .animateContentSize()
                    .verticalScroll(rememberScrollState())
            ) {
                codeOutputList.forEach {
                    Text(
                        text = it, fontSize = 17.sp, color = if (it == "Program exit") {
                            Color.Red
                        } else {
                            Color.White
                        }
                    )
                }
            }
        }
        TextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray),
            textStyle = TextStyle(Color.White),
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = {
                editorViewModel.sendInput(input)
            })
        )
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