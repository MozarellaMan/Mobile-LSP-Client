package com.example.lsp_client.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.lsp_client.editor.files.FileNode
import com.example.lsp_client.server.getDirectory

class EditorViewModel: ViewModel() {
    private val _directory = MutableLiveData(FileNode())
    val directory: LiveData<FileNode> = liveData {
        val response = getDirectory("10.0.2.2:8001")
        emit(response)
    }
}