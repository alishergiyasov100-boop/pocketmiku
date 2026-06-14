package com.korvus.pocketmiku

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.webkit.WebViewAssetLoader
import com.korvus.pocketmiku.ui.chat.AniOverlay
import com.korvus.pocketmiku.ui.theme.PocketMikuTheme
import java.io.ByteArrayInputStream

/**
 * MainActivity без Compose-Scaffold-wrapping вокруг WebView.
 *
 * Хитрая бага на HyperOS: WebView с WebGL под Compose AndroidView теряет
 * GL surface (треугольники рендерятся, но не композитятся). Решение —
 * FrameLayout как root: WebView внизу, ComposeView с Ani overlay сверху.
 */
class MainActivity : ComponentActivity() {
    private val avatarController = AvatarController()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        WebView.setWebContentsDebuggingEnabled(true)
        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportZoom(false)
            setBackgroundColor(0xFF0A0612.toInt())

            addJavascriptInterface(MikuBridge(), "MikuBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val url = request.url.toString()
                    if (url.endsWith("/miku.vrm")) {
                        return try {
                            val bytes = view.context.assets
                                .open("vrm/miku4.vrm")
                                .use { it.readBytes() }
                            WebResourceResponse(
                                "application/octet-stream", null, 200, "OK",
                                mapOf("Access-Control-Allow-Origin" to "*"),
                                ByteArrayInputStream(bytes),
                            )
                        } catch (e: Exception) {
                            Log.e("VrmAvatar", "asset open failed: $e")
                            null
                        }
                    }
                    // VRMA — отдельная обработка, AssetLoader путает MIME
                    if (url.endsWith(".vrma")) {
                        val name = url.substringAfterLast('/').removeSuffix(".vrma")
                        Log.i("VrmAvatar", "VRMA request url=$url name=$name")
                        return try {
                            val bytes = view.context.assets
                                .open("animations/$name.vrma")
                                .use { it.readBytes() }
                            Log.i("VrmAvatar", "VRMA $name size=${bytes.size}")
                            WebResourceResponse(
                                "application/octet-stream", null, 200, "OK",
                                mapOf("Access-Control-Allow-Origin" to "*"),
                                ByteArrayInputStream(bytes),
                            )
                        } catch (e: Exception) {
                            Log.e("VrmAvatar", "vrma open failed for $name: $e")
                            null
                        }
                    }
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }

            avatarController.jsExec = { js ->
                post { evaluateJavascript(js, null) }
            }

            loadUrl("https://appassets.androidplatform.net/assets/webview/index.html")
        }

        val composeOverlay = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            setContent {
                PocketMikuTheme {
                    AniOverlay(avatar = avatarController, modifier = Modifier.fillMaxSize())
                }
            }
        }

        root.addView(webView)
        root.addView(composeOverlay)
        setContentView(root)
    }
}

class AvatarController {
    internal var jsExec: ((String) -> Unit)? = null
    fun setLipSync(amplitude: Float) {
        jsExec?.invoke("Miku.setLipSync(${amplitude.coerceIn(0f, 1f)});")
    }
    fun setExpression(name: String, value: Float) {
        jsExec?.invoke("Miku.setExpression('$name', ${value.coerceIn(0f, 1f)});")
    }
    fun lookAt(yawDeg: Float, pitchDeg: Float) {
        jsExec?.invoke("Miku.setLookAt($yawDeg, $pitchDeg);")
    }
    fun playGesture(name: String, durationSec: Float = 2.0f) {
        jsExec?.invoke("Miku.playGesture('$name', $durationSec);")
    }
    /** Передать base64 FBX от Hunyuan в WebView для парсинга и retarget. */
    fun playAiMotion(fbxBase64: String, label: String = "ai") {
        val escaped = label.replace("'", "")
        jsExec?.invoke("Miku.playAiMotion('$fbxBase64', '$escaped');")
    }
}

private class MikuBridge {
    @JavascriptInterface
    fun onVrmLoaded(name: String) { Log.i("VrmAvatar", "VRM loaded: $name") }
    @JavascriptInterface
    fun onError(msg: String) { Log.e("VrmAvatar", "JS error: $msg") }
}
