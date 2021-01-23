package com.example.lsp_client

enum class ErrorCodes(val code: Int) {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS(-32602),
    INTERNAL_ERROR(-32603),
}

interface Message {
    val jsonrpc: String
}

interface RequestMessage : Message {
    val id: Int
    val method: String
    val params: List<Any?>?
}

interface ResponseMessage : Message {
    val id: Int?
    val result: Any?
    val error: ResponseError?
}

interface ResponseError {
    val code: Int
    val message: String
    val data: Any?
}
