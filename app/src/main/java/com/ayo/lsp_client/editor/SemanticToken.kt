package com.ayo.lsp_client.editor

import org.eclipse.lsp4j.Range

data class SemanticToken(
    val range: Range,
    val length: Int,
    val tokenType: String,
    val tokenModifiers: List<String>?
)
