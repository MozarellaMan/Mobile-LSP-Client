package com.ayo.lsp_client.server

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ayo.lsp_client.editor.EditorViewModel
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lsp_proxy_tools.MessageGeneratorUtil
import lsp_proxy_tools.StartLanguageServerSession
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either

fun initializeLspWebSocket(
    onSessionStart: () -> Unit,
    ipAddress: String,
    webSocketSendingScope: CoroutineScope,
    webSocketListeningScope: CoroutineScope,
    editorViewModel: EditorViewModel,
    rootUri: String,
    messageFlow: MutableSharedFlow<String>,
    messageOutputList: SnapshotStateList<String>
) {
    editorViewModel.address = ipAddress
    webSocketSendingScope.launch {
        onSessionStart()
        val session = StartLanguageServerSession(ipAddress)
        editorViewModel.outgoingSocket = session.outgoing
        editorViewModel.languageMessageDispatch = MessageGeneratorUtil(rootUri)
        editorViewModel.initialize()
        session.incoming.receiveAsFlow().collect {
            when (it) {
                is Frame.Text -> {
                    val response = it.readText()
                    editorViewModel.respond(response)
                    messageFlow.emit(response)
                }
                else -> println("unrecognized msg")
            }
        }
    }
    webSocketListeningScope.launch {
        messageFlow.collect {
            messageOutputList.add(it)
        }
    }
}

fun parseDiagnosticJson(element: JsonElement): Diagnostic {
    val diagnostic = element.jsonObject
    val range = diagnostic["range"]?.jsonObject
    val severity = diagnostic["severity"]?.jsonPrimitive?.int
    val code = diagnostic["code"]?.jsonPrimitive?.int
    val source = diagnostic["source"]?.jsonPrimitive?.content
    val message = diagnostic["message"]?.jsonPrimitive?.content

    return Diagnostic().also {
        it.range = Range().also { rnge ->
            rnge.start = Position().apply {
                line = range?.get("start")?.jsonObject?.get("line")?.jsonPrimitive?.int ?: 0
                character =
                    range?.get("start")?.jsonObject?.get("character")?.jsonPrimitive?.int ?: 0
            }
            rnge.end = Position().apply {
                line = range?.get("end")?.jsonObject?.get("line")?.jsonPrimitive?.int ?: 0
                character = range?.get("end")?.jsonObject?.get("character")?.jsonPrimitive?.int ?: 0
            }
        }
        it.severity = severity?.let { it1 -> DiagnosticSeverity.forValue(it1) }
        it.code = Either.forLeft(code.toString())
        it.source = source
        it.message = message
    }
}
