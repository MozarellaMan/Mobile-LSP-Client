package com.example.lsp_client.server

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import java.io.IOException


suspend fun testConnect(address: String): String {
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder().url(address).build()

    val result = client.newCall(request).await()
    println("code: ${result.code}: ${result.message}")
    return result.message
}

class ConnViewModel : ViewModel() {
    suspend fun getResult(ip: String): String {
        return connect(ip)
    }

    private suspend fun connect(ip: String): String {
        return try {
            testConnect("http://$ip/health")
        } catch (e: IOException) {
            e.printStackTrace()
            e.toString()
            return "Connection failed"
        }
    }
}
