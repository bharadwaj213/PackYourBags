package com.secpro.packyourbags.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ShuttleAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

data class ChatCompletionRequest(
    val model: String = "shuttle-3.5", // Using s1-mini, an official ShuttleAI model for free tier
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
) 