package com.ayo.lsp_client.server

import androidx.lifecycle.ViewModel
import com.ayo.lsp_client.editor.files.FileNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf

private fun errorMessage(error: FuelError): String {
    return "An error of type ${error.exception} happened: ${error.message}, ${error.response}"
}

private fun proxyError(error: FuelError): String {
    val errorBody = error.response.body().asString("text/html")
    val msg = if (errorBody == "(empty)") {
        "unidentified error: ${error.response.responseMessage}"
    } else {
        errorBody
    }
    return "PROXY ERROR: $msg"
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
                error.response.body().toString()
            }
    )
}

suspend fun addInput(ip: String, inputStrings: List<String>): String {
    val (_, _, result) = Fuel.post("http://$ip/code/input")
        .body(inputStrings.joinToString(separator = "\n"))
        .awaitStringResponseResult()
    return result.fold(
        { "" },
        { error ->
            println(errorMessage(error))
            proxyError(error)
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
            proxyError(error)
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
            proxyError(error)
        }
    )
}

suspend fun killRunningProgram(ip: String): String {
    val (_, _, result) = Fuel.get("http://$ip/code/kill")
        .awaitStringResponseResult()
    return result.fold(
        { data -> data },
        { error ->
            println(errorMessage(error))
            proxyError(error)
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
            { error -> proxyError(error) }
        )
    }
}

