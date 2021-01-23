package com.example.lsp_client.server

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture


const val testInit = "{\n" +
        "    \"id\": 0,\n" +
        "    \"jsonrpc\": \"2.0\",\n" +
        "    \"method\": \"initialize\",\n" +
        "    \"params\": {\n" +
        "        \"capabilities\": {\n" +
        "            \"documentSelector\": [\n" +
        "                \"java\"\n" +
        "            ],\n" +
        "            \"synchronize\": {\n" +
        "                \"configurationSection\": \"languageServerExample\"\n" +
        "            }\n" +
        "        },\n" +
        "        \"initialization_options\": {},\n" +
        "        \"process_id\": \"Null\",\n" +
        "        \"root_path\": \"./tests/example_code_repos/test-java-repo\"\n" +
        "    }\n" +
        "}"

class LspOutStream(private val channel: SendChannel<Frame>, private val coroutineScope: CoroutineScope) : OutputStream() {

    override fun write(p0: Int) {
        coroutineScope.launch {
            channel.send(Frame.Text(p0.toChar().toString()))
        }
    }

}

 class LanguageServerSocket(
    private val address: String,
) : LanguageClient {

     lateinit var socketSession: DefaultClientWebSocketSession
     private var inputStream: InputStream = "".byteInputStream()
     private var outputStream: OutputStream = ByteArrayOutputStream()


     suspend fun startSession(coroutineScope: CoroutineScope, messageFlow: MutableSharedFlow<String>) {
         val launcher = LSPLauncher.createClientLauncher(this, inputStream, outputStream)

         val client = HttpClient(CIO) {
            install(WebSockets)
        }



         val (host, port) = address.split(':', limit = 2)

         socketSession = client.webSocketSession(
                     method = HttpMethod.Get,
                     host = host,
                     port = port.toIntOrNull() ?: 0, path = "/ls"
         )

         outputStream = LspOutStream(socketSession.outgoing, coroutineScope)


         coroutineScope {
             socketSession.incoming.consumeAsFlow().collect {
                 when(it) {
                     is Frame.Text -> {
                         val msg = it.readText()
                         messageFlow.emit(msg)
                         if (msg.startsWith("Content")) {
                             inputStream = msg.byteInputStream()
                             launcher.startListening()
                         }
                     }
                     else -> {}
                 }
             }
         }
    }

     override fun telemetryEvent(`object`: Any?) {
         TODO("Not yet implemented")
     }

     override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {
         TODO("Not yet implemented")
     }

     override fun showMessage(messageParams: MessageParams?) {
         TODO("Not yet implemented")
     }

     override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
         TODO("Not yet implemented")
     }

     override fun logMessage(message: MessageParams?) {
         println(message)
     }
 }
 suspend fun startSession(address: String): DefaultClientWebSocketSession {
    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    val (host, port) = address.split(':', limit = 2)


    return client.webSocketSession(
        method = HttpMethod.Get,
        host = host,
        port = port.toIntOrNull() ?: 0, path = "/ls"
    )


}
