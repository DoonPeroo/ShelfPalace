package com.example.shelfpalace.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {
    private val apiKey = "YOUR_GEMINI_API_KEY_HERE"
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun analyzeGameCover(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        if (apiKey == "YOUR_GEMINI_API_KEY_HERE") return@withContext null
        
        try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text("This is a photo of a retro video game cover. Please identify the game title, platform, and give a very brief 1-sentence description. Format as: Title | Platform | Description")
                }
            )
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
