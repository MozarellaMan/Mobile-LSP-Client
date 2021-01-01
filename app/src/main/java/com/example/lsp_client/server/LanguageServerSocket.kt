package com.example.lsp_client.server

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class LanguageServerSocket(coroutineScope: CoroutineScope, address: String) {
    private lateinit var socketSession: DefaultClientWebSocketSession

    init {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }

        val (host, port) = address.split(':', limit = 2)

        coroutineScope.launch(Dispatchers.IO) {
            socketSession = client.webSocketSession(method = HttpMethod.Get, host = host, port = port.toIntOrNull() ?: 0, path = "/ls")
        }
    }

    fun getIncoming(): Flow<Frame> {
        return socketSession.incoming.consumeAsFlow()
    }

    suspend fun send(msg: Frame) {
        socketSession.outgoing.send(msg)
    }

    fun isOpen(): Boolean {
        return socketSession.isActive
    }

}


suspend fun testLSPSession(address: String, outgoing: Channel<String>) {
    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    val (host, port) = address.split(':', limit = 2)

    client.ws(
        method = HttpMethod.Get,
        host = host,
        port = port.toIntOrNull() ?: 0, path = "/ls"
    ) {
        when (val frame = incoming.receive()) {
            is Frame.Text -> outgoing.send(frame.readText())
            else -> println("Unrecognized message type.")
        }
    }
}
