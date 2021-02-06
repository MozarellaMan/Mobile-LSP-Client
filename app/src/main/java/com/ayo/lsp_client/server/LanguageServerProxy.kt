package com.ayo.lsp_client.server

import androidx.lifecycle.ViewModel
import com.ayo.lsp_client.editor.files.FileNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun errorMessage(error: FuelError): String {
    return "An error of type ${error.exception} happened: ${error.message}, ${error.response}"
}

@Serializable
enum class FileSyncType {
    New,
    Update,
    Delete
}

@Serializable
data class FileSyncMsg(val reason: FileSyncType, val name: String, val text: String)

suspend fun getDirectory(ip: String): FileNode {
    val (_, _, result) = Fuel.get("http://$ip/code/directory")
        .awaitObjectResponseResult<FileNode>(kotlinxDeserializerOf())
    return result.fold(
        { data -> data},
        { error ->
            println(errorMessage(error))
            FileNode()
        }
    )
}

suspend fun getFile(ip: String,path: String): String {
    val (_, _, result) = Fuel.get("http://$ip/code/file/$path")
            .awaitStringResponseResult()
    return result.fold(
            { data -> data },
            { error ->
                println(errorMessage(error))
                "No file found."
            }
    )
}

@Deprecated("Made redundant by LSP textDocument/didChange notification")
suspend fun editFile(ip: String, path: String, edits: String, fileName: String): Boolean {
    val (_, _, result) = Fuel.post("http://$ip/code/file/$path")
        .jsonBody(Json.encodeToString(FileSyncMsg(FileSyncType.Update, fileName, edits)))
        .awaitStringResponseResult()
    return result.fold(
        { data ->
            println(data)
            true
        },
        { error: FuelError ->
            println(errorMessage(error))
            false
        }
    )
}

@Deprecated("Made redundant by LSP workspace/didCreateFiles notification")
suspend fun newFile(ip: String, path: String, fileName: String): String {
    val (_, _, result) = Fuel.post("http://$ip/code/file/$path")
        .jsonBody(Json.encodeToString(FileSyncMsg(FileSyncType.New, fileName, " ")))
        .awaitStringResponseResult()
    return result.fold(
        { data ->
            data
        },
        { error: FuelError ->
            println(errorMessage(error))
            "File could not be created"
        }
    )
}

suspend fun getRootUri(ip: String): String {
    val (_, _, result) = Fuel.get("http://$ip/code/directory/root")
        .awaitStringResponseResult()
    return result.fold(
        { data -> data },
        { error ->
            println(errorMessage(error))
            ""
        }
    )
}

suspend fun runFile(ip: String,path: String): String {
    val (_, _, result) = Fuel.get("http://$ip/code/run/$path")
        .awaitStringResponseResult()
    return result.fold(
        { data -> data },
        { error ->
            println(errorMessage(error))
            "No output found."
        }
    )
}

class ConnViewModel : ViewModel() {
    suspend fun getResult(ip: String): String {
        return connect(ip)
    }

    private suspend fun connect(ip: String): String {
        val (_, _, result) = Fuel.get("http://$ip/health").awaitStringResponseResult()
        return result.fold(
            { "OK ✅" },
            { error -> errorMessage(error) }
        )
    }
}

