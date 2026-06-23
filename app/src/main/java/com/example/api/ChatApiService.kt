package com.example.api

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatApiService {
    @POST("api/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String = "openai/gpt-oss-20b:free",
    val messages: List<ChatMessageDto>
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val choices: List<ChatChoice>?
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    val message: ChatMessageDto?
)
