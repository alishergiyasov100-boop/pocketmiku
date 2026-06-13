package com.korvus.pocketmiku.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.korvus.pocketmiku.AvatarController

@Composable
fun ChatScreen(
    avatar: AvatarController,
    vm: ChatViewModel = viewModel(),
) {
    var input by remember { mutableStateOf("") }
    val state = vm.state.value
    val scrollState = rememberScrollState()

    // Авто-скролл вниз когда новое сообщение
    LaunchedEffect(state.messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Примитивный sentiment по последнему ответу Miku → VRMExpression
    // (TODO в v0.3: LLM сама теггает эмоции в response)
    LaunchedEffect(state.messages.lastOrNull()?.text) {
        val last = state.messages.lastOrNull { !it.isUser } ?: return@LaunchedEffect
        val t = last.text.lowercase()
        when {
            t.contains("ха") || t.contains("ура") || t.contains("люблю") ->
                avatar.setExpression("happy", 0.8f)
            t.contains("грустно") || t.contains("прости") ->
                avatar.setExpression("sad", 0.7f)
            t.contains("эээ") || t.contains("что?") ->
                avatar.setExpression("surprised", 0.7f)
            else -> avatar.setExpression("neutral", 1.0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xCC14141C))
                .padding(12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (state.messages.isEmpty()) {
                Text(
                    "Привет, Мастер ☆ Напиши мне что-нибудь!",
                    color = Color(0xFF36DBC0),
                )
            }
            for (m in state.messages) ChatBubble(m)
            if (state.thinking) {
                Text("Мику печатает…", color = Color(0x88FFFFFF))
            }
            state.error?.let {
                Text("× $it", color = Color(0xFFFF7676))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Скажи Мику…") },
                singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xCC14141C),
                    unfocusedContainerColor = Color(0xCC14141C),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val t = input.trim()
                    if (t.isNotEmpty()) { vm.send(t); input = "" }
                }),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                enabled = input.isNotBlank() && !state.thinking,
                onClick = {
                    val t = input.trim()
                    if (t.isNotEmpty()) { vm.send(t); input = "" }
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = Color(0xFF36DBC0),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    Row(
        horizontalArrangement = if (m.isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val bg = if (m.isUser) Color(0xFF2A4A48) else Color(0xFF3A3A4A)
        val fg = if (m.isUser) Color(0xFFC0F8E8) else Color(0xFFFFFFFF)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(m.text, color = fg)
        }
    }
}
