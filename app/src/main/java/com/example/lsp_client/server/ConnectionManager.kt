package com.example.lsp_client.server

import androidx.lifecycle.ViewModel
import com.example.lsp_client.editor.files.FileNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf

private fun errorMessage(error: FuelError): String {
    return "An error of type ${error.exception} happened: ${error.message}"
}

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

class ConnViewModel : ViewModel() {
    suspend fun getResult(ip: String): String {
        return connect(ip)
    }

    private suspend fun connect(ip: String): String {
        val (_, _, result) = Fuel.get("http://$ip/health").awaitStringResponseResult()
        return result.fold(
            { "OK ✅" },
            { error -> errorMessage(error)}
        )
    }
}
