package com.example.lsp_client.server

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage

val initialized = NotificationMessage().apply {
    method = "initialized"
    params = InitializedParams().apply {
    }
}

class LanguageMessageDispatch(private var baseUri: String,private var id: Int = -1) {
    private val gson = Gson()
    private val fileVersionMap = mutableMapOf<String, Int>()

    fun semanticTokenLegend(): Pair<Int, String> {
        val semanticTokenLegend = RequestMessage().also {
            it.method = "workspace/executeCommand"
            it.params = ExecuteCommandParams().apply {
                command = "java.project.getSemanticTokensLegend"
            }
        }
        return Pair(++id, gson.toJsonTree(semanticTokenLegend).asJsonObject.apply {
            addProperty("id","$id")
        }.toString())
    }

    fun semanticTokens(filePath: String): String {
        val semanticTokens = RequestMessage().also {
            it.method = "workspace/executeCommand"
            it.params = ExecuteCommandParams().apply {
                command = "java.project.provideSemanticTokens"
                arguments = listOf("$baseUri$filePath")
            }
        }
        return gson.toJsonTree(semanticTokens).asJsonObject.apply {
            addProperty("id","${++id}")
        }.toString()
    }

    fun textDidChange(filePath: String, content: String): String {
        val prevVersion = fileVersionMap[filePath] ?: 0
        fileVersionMap[filePath] = prevVersion
        val notification = NotificationMessage().apply {
            method = "textDocument/didChange"
            params = DidChangeTextDocumentParams().apply {
                textDocument = VersionedTextDocumentIdentifier().apply {
                    version = fileVersionMap[filePath]!!
                }
                contentChanges = listOf(TextDocumentContentChangeEvent().apply {
                    range = null
                    text = content
                })
            }
        }
        return gson.toJson(notification)
    }

    fun textDocOpen(filePath: String, content: String): String {
        var versionId = fileVersionMap.getOrPut(filePath, { 0 })
        versionId++
        fileVersionMap[filePath] = versionId
        val notification = NotificationMessage().apply {
            method = "textDocument/didOpen"
            params = DidOpenTextDocumentParams().apply {
                textDocument = TextDocumentItem().apply {
                    uri = "$baseUri$filePath"
                    languageId = "java"
                    version = versionId
                    text = content
                }
            }
        }
        return gson.toJson(notification)
    }

    fun initialize(capabilities: List<String>): String {
        return "{\n" +
                "    \"id\": ${++id},\n" +
                "    \"jsonrpc\": \"2.0\",\n" +
                "    \"method\": \"initialize\",\n" +
                "    \"params\": {\n" +
                "        \"capabilities\": {\n" +
                "            \"documentSelector\": [\n" +
                "                \"java\"\n" +
                "            ],\n" +
                "            ${capabilities.joinToString { "" }}" +
                "            \"synchronize\": {\n" +
                "                \"configurationSection\": \"languageServerExample\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"initialization_options\": {},\n" +
                "        \"process_id\": \"Null\",\n" +
                "        \"rootUri\": \"$baseUri\"\n" +
                "    }\n" +
                "}"
    }
}

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
