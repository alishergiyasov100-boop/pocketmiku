package com.korvus.pocketmiku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF36DBC0)
private val Cyan = Color(0xFF66E6FF)
private val Dark = Color(0xFF0D0D12)
private val DarkSurface = Color(0xFF14141C)

private val MikuDark = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    secondary = Cyan,
    background = Dark,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
)

private val MikuLight = lightColorScheme(
    primary = Teal,
    secondary = Cyan,
)

@Composable
fun PocketMikuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) MikuDark else MikuLight
    MaterialTheme(colorScheme = scheme, content = content)
}
