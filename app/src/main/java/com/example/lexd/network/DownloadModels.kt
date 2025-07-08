package com.example.lexd.network

data class DownloadRequest(
    val url: String,
    val mode: String
)

data class DownloadResponse(
    val filename: String,
    val file_url: String
)