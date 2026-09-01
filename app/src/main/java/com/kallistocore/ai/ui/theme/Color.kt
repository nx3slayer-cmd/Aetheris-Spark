package com.kallistocore.ai.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class KallistoColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardBackground: Color,
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val accentWave: Color,
    val userBubble: Color,
    val aiBubble: Color,
    val statusSuccess: Color,
    val error: Color
)

// 1. Midnight Dark Theme
val MidnightDarkColors = KallistoColors(
    background = Color(0xFF090A0F),
    surface = Color(0xFF12141F),
    surfaceVariant = Color(0xFF1B1E2E),
    cardBackground = Color(0xFF151827),
    primary = Color(0xFF6366F1),
    primaryVariant = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    border = Color(0xFF262B40),
    accentWave = Color(0xFF38BDF8),
    userBubble = Color(0xFF4F46E5),
    aiBubble = Color(0xFF161926),
    statusSuccess = Color(0xFF10B981),
    error = Color(0xFFEF4444)
)

// 2. Minimalist Light Theme
val MinimalistLightColors = KallistoColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    cardBackground = Color(0xFFFFFFFF),
    primary = Color(0xFF0F172A),
    primaryVariant = Color(0xFF334155),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    border = Color(0xFFE2E8F0),
    accentWave = Color(0xFF2563EB),
    userBubble = Color(0xFF0F172A),
    aiBubble = Color(0xFFF1F5F9),
    statusSuccess = Color(0xFF059669),
    error = Color(0xFFDC2626)
)

// 3. E-Ink True Black (Pure OLED #000000)
val EInkTrueBlackColors = KallistoColors(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF121212),
    cardBackground = Color(0xFF0D0D0D),
    primary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF000000),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAAAAAA),
    border = Color(0xFF2E2E2E),
    accentWave = Color(0xFFFFFFFF),
    userBubble = Color(0xFF222222),
    aiBubble = Color(0xFF111111),
    statusSuccess = Color(0xFFFFFFFF),
    error = Color(0xFFFFFFFF)
)

// 4. Nord Forest Theme
val NordForestColors = KallistoColors(
    background = Color(0xFF1C2120),
    surface = Color(0xFF242C2A),
    surfaceVariant = Color(0xFF2E3835),
    cardBackground = Color(0xFF26302D),
    primary = Color(0xFFA3BE8C),
    primaryVariant = Color(0xFF8FBCBB),
    onPrimary = Color(0xFF1C2120),
    textPrimary = Color(0xFFECEFF4),
    textSecondary = Color(0xFF889793),
    border = Color(0xFF3A4743),
    accentWave = Color(0xFF88C0D0),
    userBubble = Color(0xFF3A4743),
    aiBubble = Color(0xFF242C2A),
    statusSuccess = Color(0xFFA3BE8C),
    error = Color(0xFFBF616A)
)

enum class AppThemeSetting {
    MIDNIGHT_DARK,
    MINIMALIST_LIGHT,
    E_INK_BLACK,
    NORD_FOREST
}

val LocalKallistoColors = staticCompositionLocalOf { MidnightDarkColors }
