package com.korvus.pocketmiku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.korvus.pocketmiku.avatar.VrmAvatarView
import com.korvus.pocketmiku.ui.chat.ChatScreen
import com.korvus.pocketmiku.ui.theme.PocketMikuTheme

class MainActivity : ComponentActivity() {
    private val avatarController = AvatarController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketMikuTheme {
                Scaffold(
                    containerColor = Color(0xFF0D0D12),
                ) { padding ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFF0D0D12))) {

                        // VRM Miku-аватар — фон, занимает весь экран
                        VrmAvatarView(
                            modifier = Modifier.fillMaxSize(),
                            controller = avatarController,
                        )

                        // Чат — overlay снизу
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        ) {
                            ChatScreen(
                                avatar = avatarController,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Мост между Compose UI и WebView VRM-сценой. Compose дёргает методы,
 * VrmAvatarView пробрасывает их в JavaScript через WebView.evaluateJavascript.
 */
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
}
