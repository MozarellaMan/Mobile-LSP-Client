package com.ayo.lsp_client.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayo.lsp_client.server.parseDiagnosticJson
import com.ayo.lsp_client.ui.theming.purple200
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
import org.eclipse.lsp4j.*

class EditorViewModel(var address: String = "") : ViewModel() {
    val directory = MutableLiveData<FileNode>()
    var outgoingSocket: SendChannel<Frame> = Channel()
    var currentFile = MutableLiveData<String>()
    var languageMessageDispatch: MessageGeneratorUtil? = null
    private val gson: Gson = GsonBuilder().setLenient().create()
    private var initialized = false
    var diagnostics = mutableStateListOf<Diagnostic>()
    var semanticTokens = mutableStateListOf<SemanticToken>()
    var highestDiagnosticSeverity = MutableLiveData<Color>()
    val isCodeLoading = MutableLiveData<Boolean>()
    private var currentCodeSocket: DefaultWebSocketSession? = null
    private var semanticTokensLegendRequestSent = false
    private var semanticTokensLegend: SemanticTokensLegend? = null
    private var semanticTokensLegendReqId: Int? = null
    private var currentFileSemanticTokensReqId: Int? = null
    private var currentSemanticTokens: SemanticTokens? = null
    private var currentHoverInformation: Hover? = null
    private var hoverRequestId: Int? = null
    private var hoverReqestSent = false
    private var previousFilePath = ""
    var currentPath: String = ""
        set(newValue) {
            previousFilePath = currentPath
            field = newValue
            if (previousFilePath.isBlank() || previousFilePath == field) return
            languageMessageDispatch?.let {
                sendToSocket(it.textDocClose(previousFilePath))
                semanticTokens.clear()
                diagnostics.clear()
            }
        }

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
                getSemanticTokens()
            }
            hoverRequestId != null && webSocketMessage.contains("\"id\":\"$hoverRequestId\"") -> {
                val message = webSocketMessage.split("Content-Length:")[0]
                val responseObj = gson.fromJson(message, JsonObject::class.java)
                val result = responseObj.get("result").toString()
                currentHoverInformation = gson.fromJson(result, Hover::class.java)
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
                getSemanticTokens()
            }
        }
        getSemanticTokens()
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
            if (previousFilePath == currentPath) return
            viewModelScope.launch {
                sendToSocket(it.textDocOpen(currentPath, getFile(address, currentPath)))
                attemptSemanticLegendRequest()
                attemptSemanticTokensRequest()
                getSemanticTokens()
            }
        }
    }

    fun getHoverInfo(line: Int, character: Int) {
        languageMessageDispatch?.let {
            if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return

        }
    }

    fun editCurrentFile(edits: String) {
        languageMessageDispatch?.let {
            highestDiagnosticSeverity.value = Color.White
            diagnostics.clear()
            sendToSocket(it.textDidChange(currentPath, edits)) { currentFile.value = edits }
            getSemanticTokens()
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
            getSemanticTokens()
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
        diagnostics.clear()
        val message = webSocketMessage.split("Content-Length:")[0]
        val diagnosticsNotification = Json.parseToJsonElement(message)
        val diagnosticsJsonParams = diagnosticsNotification.jsonObject["params"]
        val diagnosticJsonUri =
            diagnosticsJsonParams?.jsonObject?.get("uri")?.jsonPrimitive?.content
        if (diagnosticJsonUri.isNullOrEmpty()) {
            return
        } else if (!diagnosticJsonUri.endsWith(currentPath)) {
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
        diagnostics.addAll(newDiagnostics)
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
        if (diagnostics.isNullOrEmpty()) {
            highestDiagnosticSeverity.value = Color.White
        }
        highestDiagnosticSeverity.value =
            colorFromDiagnosticSeverity(diagnostics.minByOrNull { it.severity.value }?.severity)
    }

    private fun getSemanticTokens() {
        semanticTokens.clear()
        semanticTokens.addAll(calculateSemanticTokens())
    }

    private fun calculateSemanticTokens(): List<SemanticToken> {
        if (semanticTokensLegend == null || currentSemanticTokens == null || currentFile.value == null) {
            return emptyList()
        }
        val newSemanticTokens = mutableListOf<SemanticToken>()
        val data = currentSemanticTokens!!.data

        var line = 0
        var start = 0
        for (i in 0 until data.size step 5) {
            val deltaLine = data[i]
            val deltaStart = data[i + 1]
            val length = data[i + 2]
            val tokenType = semanticTokensLegend!!.tokenTypes[data[i + 3]]

            if (deltaLine != 0) {
                line += deltaLine
                start = deltaStart
            } else {
                start += deltaStart
            }

            newSemanticTokens.add(
                SemanticToken(
                    range = Range(Position(line, start), Position(line, start + length)),
                    length,
                    tokenType,
                    tokenModifiers = null
                )
            )
        }
        return newSemanticTokens
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

    fun diagnosticInFileRange(diagnostic: Diagnostic, lineIndices: List<Int>) =
        inRange(diagnostic.range, lineIndices)

    fun inRange(range: Range, lineIndices: List<Int>): Boolean {
        return when {
            range.start.line > lineIndices.size || range.end.line > lineIndices.size -> {
                false
            }
            range.start.character > currentFile.value?.length ?: 0 -> {
                false
            }
            range.end.character > currentFile.value?.length ?: 0 -> {
                false
            }
            else -> true
        }
    }

    fun colorFromSemanticToken(token: SemanticToken): Color {
        return when (token.tokenType) {
            "class" -> purple200
            "type" -> Color.Blue
            "method" -> Color.Green
            "variable" -> Color.Cyan
            else -> Color.White
        }
    }
}