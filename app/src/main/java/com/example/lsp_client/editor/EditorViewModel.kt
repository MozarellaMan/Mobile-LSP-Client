package com.example.lsp_client.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lsp_client.editor.files.FileNode
import com.example.lsp_client.server.*
import com.google.gson.Gson
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class EditorViewModel(var address: String = "") : ViewModel() {
    val directory = MutableLiveData<FileNode>()
    private var previousFilePath = ""
    var currentPath: String = ""
        set(newValue) {
            previousFilePath = currentPath
            field = newValue
            if (previousFilePath.isNotBlank() && previousFilePath != field) {
                viewModelScope.launch {
                    outgoingSocket.send(
                        Frame.Text(
                            gson.toJson(
                                languageMessageDispatch?.textDocClose(
                                    previousFilePath
                                )
                            )
                        )
                    )
                }
            }
        }
    var outgoingSocket: SendChannel<Frame> = Channel()
    var currentFile = MutableLiveData<String>()
    val currentCodeOutput = MutableLiveData<String>()
    var languageMessageDispatch: LanguageMessageDispatch? = null
    val gson = Gson()
    var initialized = false

    fun getCurrentFile() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentFile.value = getFile(address, currentPath)
        }
    }

    fun getCodeOutput() {
        if (address.isBlank() || address.isBlank() || currentPath.isBlank()) return
        viewModelScope.launch {
            currentCodeOutput.value = runFile(address, currentPath)
        }
    }

    fun getFile() {
        languageMessageDispatch?.let {
            if (previousFilePath != currentPath) {
                viewModelScope.launch {
                    outgoingSocket.send(
                        Frame.Text(
                            it.textDocOpen(
                                currentPath,
                                getFile(address, currentPath)
                            )
                        )
                    )
                }
            }
        }
    }

    fun editCurrentFile(edits: String) {
        viewModelScope.launch {
            if (editFile(address, currentPath, edits, currentPath.split("/").last())) {
                outgoingSocket.send(
                    Frame.Text(
                        gson.toJson(
                            languageMessageDispatch?.textDidChange(
                                currentPath,
                                edits
                            )
                        )
                    )
                )
                currentFile.value = edits
            }
        }
    }

    fun createNewFile(directoryPath: String, fileName: String) {
        viewModelScope.launch {
            newFile(address, directoryPath, fileName)
            getDirectory(address)
        }
    }

    fun getFileDirectory() {
        if (address.isBlank()) return
        viewModelScope.launch {
            directory.value = getDirectory(address)
        }
    }
}