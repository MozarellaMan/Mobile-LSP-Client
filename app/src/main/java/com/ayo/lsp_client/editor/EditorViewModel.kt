package com.ayo.lsp_client.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayo.lsp_client.editor.files.FileNode
import com.ayo.lsp_client.server.*
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
        viewModelScope.launch {
            if (editFile(address, currentPath, edits, currentPath.split("/").last())) {
                outgoingSocket.send(
                    Frame.Text(
                        gson.toJson(
                            languageMessageDispatch?.textDidChange(
                                currentPath,
                                edits
                            )
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
        viewModelScope.launch {
            newFile(address, directoryPath, fileName)
            getFileDirectory()
        }
    }

    fun getFileDirectory() {
        if (address.isBlank()) return
        viewModelScope.launch {
            directory.value = getDirectory(address)
        }
    }
}