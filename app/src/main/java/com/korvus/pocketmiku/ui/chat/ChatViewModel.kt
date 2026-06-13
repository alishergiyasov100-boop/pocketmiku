package com.korvus.pocketmiku.ui.chat

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.korvus.pocketmiku.llm.LunaClient
import com.korvus.pocketmiku.llm.LunaException
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val thinking: Boolean = false,
    val error: String? = null,
)

class ChatViewModel : ViewModel() {
    private val luna = LunaClient()
    val state = mutableStateOf(ChatState())

    fun send(text: String) {
        val curr = state.value
        state.value = curr.copy(
            messages = curr.messages + ChatMessage(text, isUser = true),
            thinking = true,
            error = null,
        )
        viewModelScope.launch {
            try {
                val reply = luna.send(text)
                state.value = state.value.copy(
                    messages = state.value.messages + ChatMessage(reply, isUser = false),
                    thinking = false,
                )
            } catch (e: LunaException) {
                state.value = state.value.copy(
                    thinking = false,
                    error = e.message ?: "ошибка",
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    thinking = false,
                    error = "сеть: ${e.message}",
                )
            }
        }
    }

    fun reset() {
        luna.resetSession()
        state.value = ChatState()
    }
}
