package com.korvus.pocketmiku.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * HTTP клиент к KorvusTheExplorer/pocketwaifu-motion HF Space.
 * Дёргает Tencent Hunyuan Motion через прокси-Space.
 */
class MotionClient {
    private val baseUrl = "https://KorvusTheExplorer-pocketwaifu-motion.hf.space"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Сгенерить motion и вернуть base64-encoded FBX.
     * Текст — английский ("A girl waves shyly").
     * duration 0.5-12 сек, рекомендуется 1.5-3.
     */
    suspend fun generateMotion(
        text: String,
        durationSec: Float = 1.5f,
        cfgScale: Float = 2.0f,
    ): MotionResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("text", text)
            put("duration", durationSec.toDouble())
            put("cfg_scale", cfgScale.toDouble())
            put("seed", "0")
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$baseUrl/generate_blob")
            .header("Accept-Encoding", "gzip")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val errText = resp.body?.string()?.take(500) ?: ""
                throw MotionException("HTTP ${resp.code}: $errText")
            }
            val took = resp.header("X-Took-Sec")?.toFloatOrNull() ?: 0f
            val bytes = resp.body?.bytes() ?: throw MotionException("empty body")
            Log.i("MotionClient", "got FBX ${bytes.size}B in ${took}s for '$text'")
            MotionResult(
                fbxBytes = bytes,
                fbxBase64 = Base64.getEncoder().encodeToString(bytes),
                text = text,
                tookSec = took,
            )
        }
    }
}

data class MotionResult(
    val fbxBytes: ByteArray,
    val fbxBase64: String,
    val text: String,
    val tookSec: Float,
)

class MotionException(msg: String) : Exception(msg)
