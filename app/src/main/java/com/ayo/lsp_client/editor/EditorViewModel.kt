package com.ayo.lsp_client.editor

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayo.lsp_client.server.parseDiagnosticJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lsp_proxy_tools.*
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensLegend

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
    var languageMessageDispatch: MessageGeneratorUtil? = null
    private val gson: Gson = GsonBuilder().setLenient().create()
    private var initialized = false
    var diagnostics = MutableLiveData<List<Diagnostic>>()
    var highestDiagnosticSeverity = MutableLiveData<Color>()
    val currentCodeInput = MutableLiveData<String>()
    val isCodeLoading = MutableLiveData<Boolean>()
    private var currentCodeSocket: DefaultWebSocketSession? = null
    private var semanticTokensLegendRequestSent = false
    private var semanticTokensLegend: SemanticTokensLegend? = null
    private var semanticTokensLegendReqId: Int? = null
    private var currentFileSemanticTokensReqId: Int? = null
    private var currentSemanticTokens: SemanticTokens? = null

    suspend fun respond(webSocketMessage: String) {
        when {
            webSocketMessage.contains("language/status") && webSocketMessage.contains("Ready") && !initialized -> {
                outgoingSocket.send(Frame.Text(gson.toJson(languageMessageDispatch?.initialized())))
                initialized = true
            }
            webSocketMessage.contains("textDocument/publishDiagnostics") && webSocketMessage.contains(
                "range"
            ) -> {
                handleDiagnosticMessage(webSocketMessage)
            }
            semanticTokensLegendReqId != null && webSocketMessage.contains("\"id\":\"$semanticTokensLegendReqId\"") -> {
                val message = webSocketMessage.split("Content-Length:")[0]
                val responseObj = gson.fromJson(message, JsonObject::class.java)
                val result = responseObj.get("result").toString()
                semanticTokensLegend = gson.fromJson(result, SemanticTokensLegend::class.java)
            }
            currentFileSemanticTokensReqId != null && webSocketMessage.contains("\"id\":\"$currentFileSemanticTokensReqId\"") -> {
                val message = webSocketMessage.split("Content-Length:")[0]
                val responseObj = gson.fromJson(message, JsonObject::class.java)
                val result = responseObj.get("result").toString()
                currentSemanticTokens = gson.fromJson(result, SemanticTokens::class.java)
            }
        }
    }

    fun sendInput(input: String) {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentCodeSocket?.outgoing?.send(Frame.Text(input))
        }
    }

    fun getCurrentFile() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentFile.value = getFile(address, currentPath)
            languageMessageDispatch?.let {
                attemptSemanticLegendRequest()
                attemptSemanticTokensRequest()
            }
        }
    }

    fun runUserCode(outputList: MutableList<String>) {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return

        viewModelScope.launch {
            currentCodeSocket?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Program restarted"))

            isCodeLoading.value = true
            currentCodeSocket = runFile(address, currentPath)
            if (currentCodeSocket != null) {
                isCodeLoading.value = false
                (currentCodeSocket as DefaultClientWebSocketSession).incoming.receiveAsFlow()
                    .collect {
                        when (it) {
                            is Frame.Text -> {
                                val response = it.readText()
                                outputList.add(response)
                            }
                            else -> {
                                println(it)
                            }
                        }
                    }
                outputList.add("Program exit")
            }
        }
    }

    fun killRunningCode() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentCodeSocket?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Program killed"))
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
            sendToSocket(it.textDidChange(currentPath, edits)) { currentFile.value = edits }
        }
    }

    fun refreshDiagnostics() {
        languageMessageDispatch?.let {
            sendToSocket(it.refreshDiagnostics(currentPath))
        }
    }

    fun initialize() {
        languageMessageDispatch?.let {
            sendToSocket(
                it.initialize(
                    listOf(
                        "\"hoverProvider\" : \"true\"",
                        "\"semanticTokensProvider\" : \"true\"",
                        "\"textDocument.synchronization.dynamicRegistration\":\"true\"",
                    ),
                    "java"
                )
            )
        }
    }

    fun createNewFile(directoryPath: String, fileName: String) {
        languageMessageDispatch?.let {
            sendToSocket(it.didCreateFiles(directoryPath, fileName)) { getFileDirectory() }
        }
    }

    fun getFileDirectory() {
        if (address.isBlank()) return
        viewModelScope.launch {
            directory.value = getDirectory(address)
        }
    }


    private suspend fun attemptSemanticLegendRequest() {
        if (!semanticTokensLegendRequestSent) {
            semanticTokensLegendRequestSent = true
            val semanticTokensLegendRequest = languageMessageDispatch?.javaSemanticTokenLegend()
            val reqId = semanticTokensLegendRequest?.first
            val req = semanticTokensLegendRequest?.second
            semanticTokensLegendReqId = reqId
            req?.let { outgoingSocket.send(Frame.Text(it)) }
        }
    }

    private suspend fun attemptSemanticTokensRequest() {
        val semanticTokensRequest = languageMessageDispatch?.javaSemanticTokens(currentPath)
        val reqId = semanticTokensRequest?.first
        val req = semanticTokensRequest?.second
        currentFileSemanticTokensReqId = reqId
        req?.let { outgoingSocket.send(Frame.Text(it)) }
    }

    private fun handleDiagnosticMessage(webSocketMessage: String) {
        getHighestDiagnosticColour()
        diagnostics.value = emptyList()
        val message = webSocketMessage.split("Content-Length:")[0]
        val diagnosticsNotification = Json.parseToJsonElement(message)
        val diagnosticsJsonParams = diagnosticsNotification.jsonObject["params"]
        val diagnosticJsonUri =
            diagnosticsJsonParams?.jsonObject?.get("uri")?.jsonPrimitive?.content
        if (diagnosticJsonUri.isNullOrEmpty()) {
            return
        } else if (diagnosticJsonUri != "${languageMessageDispatch?.baseUri}/$currentPath") {
            return
        }
        val diagnosticsJsonArray = diagnosticsJsonParams.jsonObject["diagnostics"]?.jsonArray
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

    private fun getSemanticTokens(): List<SemanticToken> {
        if (semanticTokensLegend == null || currentSemanticTokens == null || currentFile.value == null) {
            return emptyList()
        }
        val semanticTokens = mutableListOf<SemanticToken>()
        val data = currentSemanticTokens!!.data

        for (i in 0 until data.size step 5) {
            semanticTokens.add(
                SemanticToken(
                    line = data[i],
                    startChar = data[i + 1],
                    length = data[i + 2],
                    tokenType = semanticTokensLegend!!.tokenTypes[data[i + 3]],
                    tokenModifiers = null
                )
            )
        }
        return semanticTokens
    }

    fun setCurrentInput(input: String) {
        currentCodeInput.value = input
    }

    /**
     * Sends message to language server socket
     */
    private fun sendToSocket(msg: String) {
        viewModelScope.launch {
            outgoingSocket.send(Frame.Text(msg))
        }
    }

    /**
     * Sends message to language server socket, then runs a callback
     */
    private fun sendToSocket(msg: String, then: () -> Unit) {
        viewModelScope.launch {
            outgoingSocket.send(Frame.Text(msg))
            then()
        }
    }

    fun diagnosticInFileRange(diagnostic: Diagnostic, lineIndices: List<Int>): Boolean {
        return when {
            diagnostic.range.start.line > lineIndices.size || diagnostic.range.end.line > lineIndices.size -> {
                false
            }
            diagnostic.range.start.character > currentFile.value?.length ?: 0 -> {
                false
            }
            diagnostic.range.end.character > currentFile.value?.length ?: 0 -> {
                false
            }
            else -> true
        }
    }
}