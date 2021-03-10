package com.ayo.lsp_client.editor

data class SemanticToken(
    val line: Int,
    val startChar: Int,
    val length: Int,
    val tokenType: String,
    val tokenModifiers: List<String>?
)
