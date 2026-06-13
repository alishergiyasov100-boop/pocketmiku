package com.korvus.pocketmiku.avatar

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.korvus.pocketmiku.AvatarController
import java.io.ByteArrayInputStream

/**
 * WebView с three-vrm сценой через WebViewAssetLoader.
 *
 * Почему AssetLoader: ES module imports требуют proper origin (file:// блокируется CORS).
 * WebViewAssetLoader сервит ассеты через https://appassets.androidplatform.net/ —
 * это валидный origin, ESM работает.
 *
 * Маршруты:
 *   /assets/webview/*   → src/main/assets/webview/*  (через AssetsPathHandler)
 *   /assets/vrm/miku.vrm → src/main/assets/vrm/miku4.vrm (ручной перехват)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VrmAvatarView(
    modifier: Modifier = Modifier,
    controller: AvatarController,
) {
    val ctx = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { c ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(c))
                .build()

            WebView(c).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportZoom(false)
                setBackgroundColor(0x00000000)

                addJavascriptInterface(MikuBridge(), "MikuBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (url.endsWith("/assets/vrm/miku.vrm")) {
                            return try {
                                val bytes = view.context.assets
                                    .open("vrm/miku4.vrm")
                                    .use { it.readBytes() }
                                WebResourceResponse(
                                    "application/octet-stream",
                                    null,
                                    200,
                                    "OK",
                                    mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                    ),
                                    ByteArrayInputStream(bytes),
                                )
                            } catch (e: Exception) {
                                Log.e("VrmAvatar", "asset open failed: $e")
                                null
                            }
                        }
                        return assetLoader.shouldInterceptRequest(request.url)
                    }
                }

                controller.jsExec = { js ->
                    post { evaluateJavascript(js, null) }
                }

                loadUrl("https://appassets.androidplatform.net/assets/webview/index.html")
            }
        },
    )
}

private class MikuBridge {
    @JavascriptInterface
    fun onVrmLoaded(name: String) {
        Log.i("VrmAvatar", "VRM loaded: $name")
    }

    @JavascriptInterface
    fun onError(msg: String) {
        Log.e("VrmAvatar", "JS error: $msg")
    }
}
