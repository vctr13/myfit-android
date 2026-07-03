package com.example.myfit.data.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// ── Request ────────────────────────────────────────────────

data class GeminiRequest(
    @SerializedName("system_instruction") val systemInstruction: GeminiPartList,
    val contents: List<GeminiContent>
)

data class GeminiPartList(
    val parts: List<GeminiPart>
)

data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// ── Response ───────────────────────────────────────────────

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// ── Models API ─────────────────────────────────────────────

data class GeminiModelsResponse(
    val models: List<GeminiModelInfo>?
)

data class GeminiModelInfo(
    val name: String,
    val displayName: String?,
    val supportedGenerationMethods: List<String>?
) {
    val shortName: String get() = name.removePrefix("models/")
}

// ── Service ────────────────────────────────────────────────

class GeminiService(private val apiKey: String, private val model: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun chat(
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val contents = buildList {
            history.forEach { (role, text) ->
                add(GeminiContent(role = role, parts = listOf(GeminiPart(text))))
            }
            add(GeminiContent(role = "user", parts = listOf(GeminiPart(userMessage))))
        }

        val requestBody = GeminiRequest(
            systemInstruction = GeminiPartList(listOf(GeminiPart(systemPrompt))),
            contents = contents
        )

        val url = modelUrl(model).toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .build()

        val httpRequest = Request.Builder()
            .url(url)
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val bodyStr = response.body?.string()
            if (!response.isSuccessful) {
                throw Exception("Gemini API ${response.code}: ${bodyStr?.take(300) ?: "нет тела"}")
            }
            val parsed = gson.fromJson(bodyStr, GeminiResponse::class.java)
            parsed.candidates
                ?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Gemini не вернул текст ответа")
        }
    }


    suspend fun fetchModels(): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .build()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful)
                throw Exception("API ${response.code}: ${body?.take(200) ?: "нет тела"}")
            gson.fromJson(body, GeminiModelsResponse::class.java)
                .models
                .orEmpty()
                .filter { it.supportedGenerationMethods?.contains("generateContent") == true }
                .sortedBy { it.shortName }
        }
    }
    companion object {
        fun modelUrl(model: String) =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
    }
}
