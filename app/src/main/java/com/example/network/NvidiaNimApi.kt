package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class NvidiaMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class NvidiaChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<NvidiaMessage>,
    @Json(name = "temperature") val temperature: Double = 0.5,
    @Json(name = "max_tokens") val maxTokens: Int = 1536,
    @Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class NvidiaChoice(
    @Json(name = "index") val index: Int,
    @Json(name = "message") val message: NvidiaMessage,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class NvidiaChatResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "choices") val choices: List<NvidiaChoice>? = null
)

interface NvidiaNimApi {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: NvidiaChatRequest
    ): NvidiaChatResponse
}
