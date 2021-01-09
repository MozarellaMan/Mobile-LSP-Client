package com.example.lsp_client.server

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


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
 class LanguageServerSocket(
    private val address: String,
) {
     private
     suspend fun startSession(): DefaultClientWebSocketSession {
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
