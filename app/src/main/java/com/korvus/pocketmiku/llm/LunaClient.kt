package com.korvus.pocketmiku.llm

import com.korvus.pocketmiku.persona.MikuPersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HTTP-клиент к локальному Luna-proxy (Termux на :8765) для Qwen3-Max.
 *
 * Multi-turn реализован через providerSessionId согласно reference_luna_proxy:
 *  - первый запрос без него
 *  - response-header `x-luna-provider-session-id` запоминаем
 *  - все следующие — с этим header'ом + body.providerSessionId, и в messages
 *    отправляем ТОЛЬКО последнее user-сообщение (Luna адаптер дропает history)
 */
class LunaClient(
    private val baseUrl: String = "http://127.0.0.1:8765",
    private val model: String = "qwen3-max-2026-01-23",
    private val apiKey: String = "luna",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Multi-turn session-id, выставляется после первого ответа. */
    private var providerSessionId: String? = null
    private var systemPromptSent: Boolean = false

    /**
     * Послать сообщение, вернуть ответ Miku. Бросает [LunaException] при ошибке.
     */
    suspend fun send(userText: String): String = withContext(Dispatchers.IO) {
        val messages = mutableListOf<ChatMessage>()
        // Первый ход — отправляем system+user; дальше — только user (history дропается)
        if (!systemPromptSent || providerSessionId == null) {
            messages += ChatMessage("system", MikuPersona.SYSTEM_PROMPT.trim())
            systemPromptSent = true
        }
        messages += ChatMessage("user", userText)

        val body = ChatRequest(
            model = model,
            messages = messages,
            temperature = 0.85,
            maxTokens = 400,
            stream = false,
            providerSessionId = providerSessionId,
        )

        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .apply {
                providerSessionId?.let { header("x-luna-provider-session-id", it) }
            }
            .post(json.encodeToString(body).toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            // Capture session-id из response-header для multi-turn
            resp.header("x-luna-provider-session-id")?.let { providerSessionId = it }

            if (!resp.isSuccessful) {
                throw LunaException("Luna ${resp.code}: ${resp.body?.string()?.take(200)}")
            }
            val txt = resp.body?.string()
                ?: throw LunaException("empty body")
            val parsed = json.decodeFromString<ChatResponse>(txt)
            parsed.choices.firstOrNull()?.message?.content?.trim()
                ?: throw LunaException("no choices")
        }
    }

    /** Сбросить сессию (новый разговор). */
    fun resetSession() {
        providerSessionId = null
        systemPromptSent = false
    }
}

class LunaException(msg: String) : Exception(msg)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    val stream: Boolean,
    val providerSessionId: String? = null,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatResponse(
    val choices: List<Choice> = emptyList(),
)

@Serializable
private data class Choice(
    val message: ChatMessage,
)
