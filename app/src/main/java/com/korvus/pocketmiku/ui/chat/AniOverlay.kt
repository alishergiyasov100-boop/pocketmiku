package com.korvus.pocketmiku.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.korvus.pocketmiku.AvatarController
import kotlinx.coroutines.delay

/**
 * Ani-style overlay: модель занимает весь экран, UI — полупрозрачные плавающие
 * элементы. Чат-пузырей нет: последняя реплика Miku показывается subtitle-ом
 * сверху и через ~7 сек угасает.
 */
@Composable
fun AniOverlay(
    avatar: AvatarController,
    modifier: Modifier = Modifier,
    vm: ChatViewModel = viewModel(),
) {
    val state = vm.state.value

    // primitive sentiment → expression (как в v0.1; LLM-теггинг будет в v0.3)
    LaunchedEffect(state.messages.lastOrNull()?.text) {
        val last = state.messages.lastOrNull { !it.isUser } ?: return@LaunchedEffect
        val t = last.text.lowercase()
        when {
            t.contains("ха") || t.contains("ура") || t.contains("люблю") ->
                avatar.setExpression("happy", 0.8f)
            t.contains("грустно") || t.contains("прости") ->
                avatar.setExpression("sad", 0.7f)
            t.contains("?!") || t.contains("эээ") ->
                avatar.setExpression("surprised", 0.7f)
            else -> avatar.setExpression("neutral", 1.0f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        TopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Subtitle(
            text = state.messages.lastOrNull { !it.isUser }?.text,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.85f)
                .statusBarsPadding()
                .padding(top = 64.dp),
        )

        BottomDeck(
            state = state,
            onSend = { vm.send(it) },
            onReset = { vm.reset() },
            onGesture = { name -> avatar.playGesture(name, 2.0f) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun TopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircle(size = 40.dp, onClick = { /* TODO menu */ }) {
            Icon(Icons.Filled.Menu, "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.weight(1f))
        GlassPill(onClick = { /* TODO capture */ }) {
            Icon(
                Icons.Filled.CameraAlt, "Capture",
                tint = Color.White, modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Capture", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Subtitle(text: String?, modifier: Modifier = Modifier) {
    // ключ-trigger — сам текст, чтобы каждое новое сообщение пере-запускало таймер
    var visible by remember(text) { mutableStateOf(text != null) }
    LaunchedEffect(text) {
        if (text == null) return@LaunchedEffect
        visible = true
        delay(7000)
        visible = false
    }
    AnimatedVisibility(
        visible = visible && !text.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2E2E3A).copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text ?: "",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BottomDeck(
    state: ChatState,
    onSend: (String) -> Unit,
    onReset: () -> Unit,
    onGesture: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    var muted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Полоска жестов (emoji-кнопки)
        GestureBar(onGesture = onGesture)

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircle(size = 48.dp, onClick = { /* TODO push-to-talk */ }) {
                Icon(Icons.Filled.Videocam, "Voice", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            GlassCircle(size = 48.dp, onClick = { muted = !muted }) {
                Icon(
                    if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    "Mic", tint = Color.White, modifier = Modifier.size(22.dp),
                )
            }
            GlassCircle(size = 48.dp, onClick = onReset) {
                Icon(Icons.Filled.Mood, "Reset", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            GlassCircle(size = 48.dp, onClick = { /* TODO settings */ }) {
                Icon(Icons.Filled.Settings, "Settings", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        AskAnythingPill(
            value = input,
            onValueChange = { input = it },
            thinking = state.thinking,
            error = state.error,
            onSend = {
                val t = input.trim()
                if (t.isNotEmpty()) {
                    onSend(t)
                    input = ""
                }
            },
        )
    }
}

@Composable
private fun AskAnythingPill(
    value: String,
    onValueChange: (String) -> Unit,
    thinking: Boolean,
    error: String?,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF3A3A48).copy(alpha = 0.82f))
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            if (value.isEmpty() && !thinking && error == null) {
                Text(
                    "Ask Anything",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp,
                )
            }
            if (thinking) {
                Text(
                    "…",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                )
            }
            error?.let {
                if (value.isEmpty()) {
                    Text("× $it", color = Color(0xFFFF8C8C), fontSize = 13.sp)
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 15.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF66E6FF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF36DBC0).copy(alpha = 0.9f))
                    .clickable(enabled = !thinking) { onSend() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private data class GestureChip(val emoji: String, val name: String, val label: String)

private val GESTURES = listOf(
    GestureChip("👋", "Goodbye", "wave"),
    GestureChip("👏", "Clapping", "clap"),
    GestureChip("⬆", "Jump", "jump"),
    GestureChip("👀", "LookAround", "look"),
    GestureChip("🤔", "Thinking", "think"),
    GestureChip("😳", "Blush", "blush"),
    GestureChip("😲", "Surprised", "wow"),
    GestureChip("😢", "Sad", "sad"),
    GestureChip("😴", "Sleepy", "sleepy"),
    GestureChip("😠", "Angry", "angry"),
    GestureChip("😌", "Relax", "relax"),
)

@Composable
private fun GestureBar(onGesture: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(GESTURES) { g ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF3A3A48).copy(alpha = 0.82f))
                    .clickable { onGesture(g.name) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(g.label, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GlassCircle(
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF3A3A48).copy(alpha = 0.82f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun GlassPill(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF3A3A48).copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
