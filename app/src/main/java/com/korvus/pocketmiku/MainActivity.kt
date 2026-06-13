package com.korvus.pocketmiku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.korvus.pocketmiku.avatar.VrmAvatarView
import com.korvus.pocketmiku.ui.chat.AniOverlay
import com.korvus.pocketmiku.ui.theme.PocketMikuTheme

class MainActivity : ComponentActivity() {
    private val avatarController = AvatarController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketMikuTheme {
                Scaffold(containerColor = Color.Black) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        VrmAvatarView(
                            modifier = Modifier.fillMaxSize(),
                            controller = avatarController,
                        )
                        AniOverlay(avatar = avatarController)
                    }
                }
            }
        }
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
}
