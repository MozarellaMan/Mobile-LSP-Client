package com.ayo.lsp_client.editor

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayo.lsp_client.server.*
import com.ayo.lsp_client.ui.editor.files.FileNode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity

class EditorViewModel(var address: String = "") : ViewModel() {
    val directory = MutableLiveData<FileNode>()
    private var previousFilePath = ""
    var currentPath: String = ""
        set(newValue) {
            previousFilePath = currentPath
            field = newValue
            if (previousFilePath.isNotBlank() && previousFilePath != field) {
                viewModelScope.launch {
                    outgoingSocket.send(
                        Frame.Text(
                            gson.toJson(
                                languageMessageDispatch?.textDocClose(
                                    previousFilePath
                                )
                            )
                        )
                    )
                }
            }
        }
    var outgoingSocket: SendChannel<Frame> = Channel()
    var currentFile = MutableLiveData<String>()
    val currentCodeOutput = MutableLiveData<String>()
    var languageMessageDispatch: LanguageMessageDispatch? = null
    private val gson: Gson = GsonBuilder().setLenient().create()
    private var initialized = false
    var diagnostics = MutableLiveData<List<Diagnostic>>()
    var highestDiagnosticSeverity = MutableLiveData<Color>()
    var programInputs = MutableLiveData<List<String>>()

    suspend fun respond(webSocketMessage: String) {
        if (webSocketMessage.contains("language/status") && webSocketMessage.contains("Ready") && !initialized) {
            outgoingSocket.send(Frame.Text(gson.toJson(languageMessageDispatch?.initialized)))
            initialized = true
        }
        if (webSocketMessage.contains("textDocument/publishDiagnostics") && webSocketMessage.contains(
                "range"
            )
        ) {
            val message = webSocketMessage.split("Content-Length:")[0]
            val diagnosticsNotification = Json.parseToJsonElement(message)
            val diagnosticsJsonArray =
                diagnosticsNotification.jsonObject["params"]?.jsonObject?.get("diagnostics")?.jsonArray
            val newDiagnostics = mutableListOf<Diagnostic>()
            diagnosticsJsonArray?.forEach {
                val diagnosticObj = parseDiagnosticJson(it)
                diagnosticObj.message?.let {
                    newDiagnostics.add(diagnosticObj)
                }
            }
            diagnostics.value = newDiagnostics
            getHighestDiagnosticColour()
        }
    }

    suspend fun sendProgramInputs() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            programInputs.value?.let { addInput(address, it) }
        }
    }

    fun getCurrentFile() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentFile.value = getFile(address, currentPath)
        }
    }

    fun getCodeOutput() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentCodeOutput.value = "Running..."
            currentCodeOutput.value = runFile(address, currentPath)
        }
    }

    fun getFile() {
        languageMessageDispatch?.let {
            if (previousFilePath != currentPath) {
                viewModelScope.launch {
                    outgoingSocket.send(
                        Frame.Text(
                            it.textDocOpen(
                                currentPath,
                                getFile(address, currentPath)
                            )
                        )
                    )
                }
            }
        }
    }

    fun editCurrentFile(edits: String) {
        languageMessageDispatch?.let {
            viewModelScope.launch {
                outgoingSocket.send(
                    Frame.Text(
                        it.textDidChange(
                            currentPath,
                            edits
                        )
                    )
                )
                currentFile.value = edits
            }
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            languageMessageDispatch?.let { Frame.Text(it.refreshDiagnostics(currentPath)) }?.let {
                outgoingSocket.send(
                    it
                )
            }
        }
    }

    fun initialize() {
        viewModelScope.launch {
            languageMessageDispatch?.let {
                Frame.Text(
                    it.initialize(
                        listOf(
                            "\"hoverProvider\" : \"true\"",
                            "\"textDocument.synchronization.dynamicRegistration\":\"true\""
                        )
                    )
                )
            }?.let {
                outgoingSocket.send(
                    it
                )
            }
        }
    }

    fun createNewFile(directoryPath: String, fileName: String) {
        languageMessageDispatch?.let {
            viewModelScope.launch {
                outgoingSocket.send(Frame.Text(it.didCreateFiles(directoryPath, fileName)))
                getFileDirectory()
            }
        }
    }

    fun getFileDirectory() {
        if (address.isBlank()) return
        viewModelScope.launch {
            directory.value = getDirectory(address)
        }
    }

    fun colorFromDiagnosticSeverity(diagnosticSeverity: DiagnosticSeverity?): Color {
        return when (diagnosticSeverity) {
            DiagnosticSeverity.Error -> Color.Red
            DiagnosticSeverity.Warning -> Color.Yellow
            DiagnosticSeverity.Information -> Color.Blue
            DiagnosticSeverity.Hint -> Color.Green
            else -> Color.White
        }
    }

    private fun getHighestDiagnosticColour() {
        if (diagnostics.value.isNullOrEmpty()) {
            highestDiagnosticSeverity.value = Color.White
        }
        highestDiagnosticSeverity.value =
            colorFromDiagnosticSeverity(diagnostics.value?.minByOrNull { it.severity.value }?.severity)
    }
}