package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AIEngine {
    private const val TAG = "AIEngine"

    private const val SYSTEM_INSTRUCTION = """
You are an expert financial assistant. Your task is to analyze plain text input (primarily in Norwegian) and categorize it into either a 'transaction' (financial expense or income) or a 'note' (general text note).
Respond ONLY with a JSON object that matches this schema:
{
  "result_type": "transaction" | "note" | "unrecognized",
  "transaction": {
    "type": "INNTEKT" | "UTGIFT",
    "amount": number,
    "category": "string representing category (e.g. Salg, Varekjøp, Diverse, Reise, Maskiner)",
    "description": "short description of what was bought or sold"
  },
  "note": {
    "title": "short note title",
    "content": "detailed note content"
  },
  "feedback_message": "A short, friendly confirmation message in Norwegian (e.g. 'Registrerte salg av kaffe på 150 kr!')"
}

Rules:
1. If the input contains a price/amount and mentions selling, earning, getting money, or sales, set result_type to 'transaction' and type to 'INNTEKT'.
2. If the input contains a price/amount and mentions buying, spending, purchasing, or expenses, set result_type to 'transaction' and type to 'UTGIFT'.
3. If the input has no monetary amount but has some task, memo, or note context, set result_type to 'note' and extract a suitable title and the text content.
4. If the input is completely unintelligible or empty, set result_type to 'unrecognized' and set feedback_message to a friendly message in Norwegian asking for clarification.
5. Provide the category and description in Norwegian if possible. Common categories: 'Salg', 'Varekjøp', 'Tjenester', 'Kontor', 'Markedsføring', 'Reise', 'Diverse'.
6. Do not include markdown code blocks like ```json. Return ONLY raw JSON.
"""

    suspend fun parseInput(input: String): GeminiParseResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext GeminiParseResult(
                result_type = "unrecognized",
                feedback_message = "Gemini API-nøkkel mangler. Vennligst konfigurer den i AI Studio-panelet."
            )
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = input))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))
            )
        )

        try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Gemini Response: $jsonText")
                return@withContext GeminiClient.parseResultAdapter.fromJson(jsonText)
            } else {
                Log.e(TAG, "Empty response from Gemini")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            return@withContext GeminiParseResult(
                result_type = "unrecognized",
                feedback_message = "Kunne ikke kontakte AI: ${e.localizedMessage ?: "Nettverksfeil"}"
            )
        }
    }
}
