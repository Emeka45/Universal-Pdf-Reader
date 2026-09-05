package com.coeric.universalreader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** A small provider-neutral request used by the in-app AI assistant. */
data class AiRequest(
    val question: String,
    val documentContext: String = ""
)

data class AiResponse(
    val text: String,
    val isDemo: Boolean = false
)

object AiService {
    private const val PREFS = "universal_reader_ai"
    private const val ENDPOINT = "endpoint"
    private const val API_KEY = "api_key"
    private const val MODEL = "model"

    fun getEndpoint(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ENDPOINT, "") ?: ""

    fun getApiKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(API_KEY, "") ?: ""

    fun getModel(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(MODEL, "gpt-5.6-luna") ?: "gpt-5.6-luna"

    fun saveSettings(context: Context, endpoint: String, apiKey: String, model: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ENDPOINT, endpoint.trim())
            .putString(API_KEY, apiKey.trim())
            .putString(MODEL, model.trim().ifBlank { "gpt-5.6-luna" })
            .apply()
    }

    suspend fun ask(context: Context, request: AiRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        val endpoint = getEndpoint(context)
        if (endpoint.isBlank()) {
            return@withContext Result.success(AiResponse(demoAnswer(request), isDemo = true))
        }

        runCatching {
            val body = JSONObject().apply {
                put("model", getModel(context))
                put("input", buildPrompt(request))
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                val key = getApiKey(context)
                if (key.isNotBlank()) setRequestProperty("Authorization", "Bearer $key")
            }

            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            connection.disconnect()

            if (code !in 200..299) error("AI server returned HTTP $code: ${responseText.take(500)}")
            AiResponse(extractText(responseText))
        }
    }

    private fun buildPrompt(request: AiRequest): String = buildString {
        append("You are Universal Reader AI, an expert reading, research and study assistant. ")
        append("Answer accurately and clearly. If document context is supplied, ground the answer in it and say when the context is insufficient.\n\n")
        if (request.documentContext.isNotBlank()) {
            append("DOCUMENT CONTEXT:\n")
            append(request.documentContext.take(60_000))
            append("\n\n")
        }
        append("USER QUESTION:\n")
        append(request.question.trim())
    }

    private fun extractText(raw: String): String {
        val json = JSONObject(raw)
        val direct = json.optString("output_text")
        if (direct.isNotBlank()) return direct

        val output = json.optJSONArray("output")
        if (output != null) {
            val parts = mutableListOf<String>()
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val piece = content.optJSONObject(j) ?: continue
                    val text = piece.optString("text")
                    if (text.isNotBlank()) parts += text
                }
            }
            if (parts.isNotEmpty()) return parts.joinToString("\n")
        }

        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val message = choices.optJSONObject(0)?.optJSONObject("message")
            val text = message?.optString("content") ?: ""
            if (text.isNotBlank()) return text
        }
        error("The AI server returned an unreadable response.")
    }

    private fun demoAnswer(request: AiRequest): String {
        val q = request.question.trim()
        return if (request.documentContext.isBlank()) {
            "AI Demo Mode is active. Your question was: \"$q\"\n\nConnect an AI-compatible endpoint in AI Settings to enable live model answers."
        } else {
            "AI Demo Mode received your question and document context.\n\nQuestion: $q\n\nThe live AI connection is not configured yet, so I won't pretend this is a model-generated answer. Configure the endpoint in AI Settings to analyze the document context."
        }
    }
}
