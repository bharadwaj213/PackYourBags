package com.secpro.packyourbags.api

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ShuttleAIService(private val context: Context) {
    private val TAG = "ShuttleAIService"
    private val BASE_URL = "https://api.shuttleai.app/"
    
    // Replace "sk-your-api-key-here" with your actual ShuttleAI API key
    // Format should be: "Bearer sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    private val DEFAULT_API_KEY = "Bearer shuttle-e08275ca9fa59b862a8c"
    
    private val api: ShuttleAIApi
    
    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        api = retrofit.create(ShuttleAIApi::class.java)
    }
    
    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("PackYourBags", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("shuttle_ai_key", null)
        
        return if (!savedKey.isNullOrBlank()) {
            "Bearer $savedKey"
        } else {
            DEFAULT_API_KEY
        }
    }
    
    suspend fun generatePackingRecommendations(
        destination: String,
        weatherDesc: String,
        tripDuration: Int
    ): List<String> {
        try {
            Log.d(TAG, "⭐ Starting ShuttleAI request for destination: $destination")
            Log.d(TAG, "⭐ Weather description: $weatherDesc")
            Log.d(TAG, "⭐ Trip duration: $tripDuration days")
            
            // Create a system message for the AI
            val systemMessage = Message(
                role = "system",
                content = "You are an expert travel assistant that creates practical packing lists based on destination, weather, and trip duration."
            )
            
            // Create a user prompt that explains what we need
            val userPrompt = """
                Create a packing list for a ${tripDuration}-day trip to ${destination}.
                
                Weather information: ${weatherDesc}
                
                Format each item on a new line, starting with a dash, without any additional commentary.
                Example format:
                - Item 1
                - Item 2
                - Item 3
                
                Be comprehensive but practical. Don't suggest packing excessive items for short trips.
                Include clothing appropriate for the weather, toiletries, electronics, documents, and any destination-specific items.
            """.trimIndent()
            
            val userMessage = Message(role = "user", content = userPrompt)
            
            // Log the complete prompt being sent
            Log.d(TAG, "⭐ ShuttleAI prompt: $userPrompt")
            
            // Create the request
            val request = ChatCompletionRequest(
                messages = listOf(systemMessage, userMessage)
            )
            
            // Log the API key being used (masked for security)
            val apiKey = getApiKey()
            val maskedKey = if (apiKey.length > 10) 
                "${apiKey.substring(0, 10)}..." 
            else 
                "Invalid key format"
            Log.d(TAG, "⭐ Using API key: $maskedKey")
            
            // Make the API call with the stored API key
            Log.d(TAG, "⭐ Making API request to ShuttleAI")
            val response = api.createChatCompletion(apiKey, request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "⭐ ShuttleAI response successful: ${response.code()}")
                val result = response.body()
                val content = result?.choices?.firstOrNull()?.message?.content ?: ""
                
                // Log the raw response from ShuttleAI
                Log.d(TAG, "⭐ ShuttleAI raw response: $content")
                
                // Parse the content into individual items
                val items = content.lines()
                    .filter { it.trim().startsWith("-") }
                    .map { it.trim().removePrefix("-").trim() }
                    .filter { it.isNotEmpty() }
                
                Log.d(TAG, "⭐ Parsed ${items.size} items from ShuttleAI response")
                items.forEachIndexed { index, item ->
                    Log.d(TAG, "⭐ Item ${index+1}: $item")
                }
                
                return items
            } else {
                Log.e(TAG, "⭐ ShuttleAI API call failed: ${response.code()}")
                Log.e(TAG, "⭐ Error body: ${response.errorBody()?.string()}")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "⭐ Error generating packing recommendations: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    companion object {
        // For saving and retrieving API key
        fun saveApiKey(context: Context, apiKey: String) {
            val prefs = context.getSharedPreferences("PackYourBags", Context.MODE_PRIVATE)
            prefs.edit().putString("shuttle_ai_key", apiKey).apply()
        }
        
        fun getApiKey(context: Context): String {
            val prefs = context.getSharedPreferences("PackYourBags", Context.MODE_PRIVATE)
            return prefs.getString("shuttle_ai_key", "") ?: ""
        }
    }
} 