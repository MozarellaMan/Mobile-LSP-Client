package com.example.lsp_client.editor.files

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FileNode(
    val path: String = "",
    val name: String = "",
    @SerialName("type") val fileType: String = "",
    val children: List<FileNode> = emptyList(),
    @Transient var parent: FileNode? = null
) {
    fun isDirectory(): Boolean {
        return this.fileType == "directory"
    }

    fun isEmpty(): Boolean {
        return this.name.isBlank() && this.path.isBlank() && this.fileType.isBlank() && this.children.isEmpty()
    }

    fun isRoot(): Boolean {
        return this.parent == null
    }

    fun getPath(root: FileNode): String {
        return this.path.removePrefix("${root.path}/")
    }
}