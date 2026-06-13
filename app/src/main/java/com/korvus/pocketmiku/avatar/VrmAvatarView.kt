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
import com.korvus.pocketmiku.AvatarController
import java.io.ByteArrayInputStream

/**
 * WebView с three-vrm сценой.
 *
 * Перешли с ES modules + WebViewAssetLoader на single-bundle (esbuild IIFE) +
 * file:// — это убирает все CORS/origin сложности System WebView на HyperOS.
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
            WebView.setWebContentsDebuggingEnabled(true)
            WebView(c).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
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
                                    "application/octet-stream",
                                    "binary",
                                    ByteArrayInputStream(bytes),
                                )
                            } catch (e: Exception) {
                                Log.e("VrmAvatar", "asset open failed: $e")
                                null
                            }
                        }
                        return null
                    }
                }

                controller.jsExec = { js ->
                    post { evaluateJavascript(js, null) }
                }

                loadUrl("file:///android_asset/webview/index.html")
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
