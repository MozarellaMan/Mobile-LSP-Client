package com.ayo.lsp_client.server

import com.google.gson.Gson
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage

class LanguageMessageDispatch(private var baseUri: String, private var id: Int = -1) {
    private val gson = Gson()
    private val fileVersionMap = mutableMapOf<String, Int>()
    val initialized = NotificationMessage().apply {
        method = "initialized"
        params = InitializedParams().apply {
        }
    }

    fun didCreateFiles(filePath: String, fileName: String): String {
        val notification = NotificationMessage().apply {
            method = "workspace/didCreateFiles"
            params = CreateFilesParams().apply {
                files = listOf(FileCreate().apply {
                    uri = "$baseUri/$filePath/$fileName"
                })
            }
        }
        return gson.toJson(notification)
    }

    fun textDidChange(filePath: String, content: String): String {
        val prevVersion = fileVersionMap[filePath] ?: 0
        fileVersionMap[filePath] = prevVersion
        val notification = NotificationMessage().apply {
            method = "textDocument/didChange"
            params = DidChangeTextDocumentParams().apply {
                textDocument = VersionedTextDocumentIdentifier().apply {
                    uri = "$baseUri/$filePath"
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
                    uri = "$baseUri/$filePath"
                    languageId = "java"
                    version = versionId
                    text = content
                }
            }
        }
        return gson.toJson(notification)
    }

    fun textDocClose(filePath: String): String {
        var versionId = fileVersionMap.getOrPut(filePath, { 0 })
        versionId++
        fileVersionMap[filePath] = versionId
        val notification = NotificationMessage().apply {
            method = "textDocument/didClose"
            params = DidCloseTextDocumentParams().apply {
                textDocument = TextDocumentIdentifier().apply {
                    uri = "$baseUri/$filePath"
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
                "            ${capabilities.joinToString(separator = ",\n", postfix = ",\n")}" +
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

    fun refreshDiagnostics(filePath: String): String {
        val refresh = RequestMessage().also {
            it.method = "workspace/executeCommand"
            it.params = ExecuteCommandParams().apply {
                command = "java.project.refreshDiagnostics"
                arguments = listOf("$baseUri/$filePath", "thisFile", true)
            }
        }
        return gson.toJsonTree(refresh).asJsonObject.apply {
            addProperty("id", "${++id}")
        }.toString()
    }

// Not working
//    fun semanticTokenLegend(): Pair<Int, String> {
//        val semanticTokenLegend = RequestMessage().also {
//            it.method = "workspace/executeCommand"
//            it.params = ExecuteCommandParams().apply {
//                command = "java.project.getSemanticTokensLegend"
//            }
//        }
//        return Pair(++id, gson.toJsonTree(semanticTokenLegend).asJsonObject.apply {
//            addProperty("id","$id")
//        }.toString())
//    }
//
//    fun semanticTokens(filePath: String): String {
//        val semanticTokens = RequestMessage().also {
//            it.method = "workspace/executeCommand"
//            it.params = ExecuteCommandParams().apply {
//                command = "java.project.provideSemanticTokens"
//                arguments = listOf("$baseUri/$filePath")
//            }
//        }
//        return gson.toJsonTree(semanticTokens).asJsonObject.apply {
//            addProperty("id","${++id}")
//        }.toString()
//    }
}