package com.example.lsp_client.server

import androidx.lifecycle.ViewModel
import com.example.lsp_client.editor.files.FileNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf

suspend fun getDirectory(ip: String): FileNode {
    val (_, _, result) = Fuel.get("http://$ip/code/directory")
        .awaitObjectResponseResult<FileNode>(kotlinxDeserializerOf())
    return result.fold(
        { data -> data},
        { error ->
            println("An error of type ${error.exception} happened: ${error.message}")
            FileNode()
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
            { error -> "An error of type ${error.exception} happened: ${error.message}" }
        )
    }
}
