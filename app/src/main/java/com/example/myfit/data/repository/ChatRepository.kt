package com.example.myfit.data.repository

import com.example.myfit.data.db.dao.ChatMessageDao
import com.example.myfit.data.db.dao.FoodEntryDao
import com.example.myfit.data.db.dao.ProductDao
import com.example.myfit.data.db.dao.UserProfileDao
import com.example.myfit.data.db.entity.ChatMessage
import com.example.myfit.data.model.ChatResult
import com.example.myfit.data.model.ParsedFoodData
import com.example.myfit.data.network.GeminiService
import com.example.myfit.data.profile.ProfileContextBuilder
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class ChatRepository(
    private val chatMessageDao: ChatMessageDao,
    private val userProfileDao: UserProfileDao,
    private val productDao: ProductDao,
    private val foodEntryDao: FoodEntryDao,
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String,
    private val chatType: String = CHAT_MAIN
) {
    private val gson = Gson()

    fun messages(): Flow<List<ChatMessage>> = chatMessageDao.getMessages(chatType)

    suspend fun send(userText: String): ChatResult {
        chatMessageDao.insert(ChatMessage(chat_type = chatType, role = "user", content = userText))

        val profile  = userProfileDao.getProfileOnce()
        val products = productDao.getAllOnce()
        val today    = LocalDate.now().toString()
        val todayEntries = foodEntryDao.getByDateOnce(today)
        val todayTotals  = foodEntryDao.getDailyTotals(today)

        val systemPrompt = if (profile != null)
            ProfileContextBuilder.build(profile, products, todayEntries, todayTotals)
        else
            FALLBACK_PROMPT

        val todayStartMs = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val history = chatMessageDao
            .getRecentMessagesSince(chatType, sinceMs = todayStartMs, limit = HISTORY_LIMIT)
            .reversed()
            .dropLast(1)
            .map { it.role to it.content }

        val fullReply = GeminiService(apiKeyProvider(), modelProvider())
            .chat(systemPrompt, history, userText)

        val match = FOOD_DATA_REGEX.find(fullReply)
        val foodData: ParsedFoodData? = match?.let {
            try {
                val parsed = gson.fromJson(it.groupValues[1].trim(), ParsedFoodData::class.java)
                if (parsed.items.isEmpty() && parsed.waterMl == 0f) null else parsed
            } catch (e: Exception) { null }
        }
        val cleanText = if (match != null) fullReply.replace(match.value, "").trim() else fullReply

        chatMessageDao.insert(ChatMessage(chat_type = chatType, role = "model", content = cleanText))
        chatMessageDao.trimOldMessages(chatType, keepLast = KEEP_MESSAGES)

        return ChatResult(cleanText, foodData)
    }

    suspend fun clearHistory() = chatMessageDao.deleteByType(chatType)

    companion object {
        const val CHAT_MAIN = "main"
        private const val HISTORY_LIMIT = 20
        private const val KEEP_MESSAGES = 100
        private val FOOD_DATA_REGEX = Regex("""\[FOOD_DATA]([\s\S]*?)\[/FOOD_DATA]""")

        private const val FALLBACK_PROMPT =
            "Ты — персональный AI-нутрициолог. " +
            "Отвечай на русском языке. " +
            "Используй Markdown для форматирования ответов."
    }
}
