package com.ayo.lsp_client.server

import androidx.lifecycle.ViewModel
import lsp_proxy_tools.healthCheck

class ConnViewModel : ViewModel() {
    suspend fun getResult(address: String): String {
        return connect(address)
    }

    private suspend fun connect(address: String): String {
        return healthCheck(address)
    }
}