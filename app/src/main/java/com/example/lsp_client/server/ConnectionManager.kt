package com.example.lsp_client.server

import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await


suspend fun testConnect(address: String): String {
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder().url(address).build()

    val result = client.newCall(request).await()
    println("code: ${result.code}: ${result.message}")
    return result.message
}
