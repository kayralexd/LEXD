package com.example.lexd.network

import retrofit2.http.Body
import retrofit2.http.POST

// /formats için
data class FormatsRequest(val url: String)
data class FormatInfo(
    val id: String,
    val ext: String,
    val resolution: String?,
    val abr: Int?
)

// /download için

interface DownloadService {
    @POST("/formats")
    suspend fun listFormats(@Body req: FormatsRequest): List<FormatInfo>

    @POST("/download")
    suspend fun download(@Body req: DownloadRequest): DownloadResponse
}