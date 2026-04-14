package com.mtc.termosonda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Palette ──────────────────────────────────────────────────────────────────

object SondaColors {
    // Backgrounds (layered dark surfaces)
    val Background       = Color(0xFF0F0F10)
    val Surface          = Color(0xFF1A1A1E)
    val SurfaceContainer = Color(0xFF242428)
    val SurfaceVariant   = Color(0xFF2E2E34)

    // Borders
    val OutlineSubtle    = Color(0x14FFFFFF)   // 8% white
    val OutlineMuted     = Color(0x24FFFFFF)   // 14% white

    // Brand / accent
    val Accent           = Color(0xFF3B82F6)   // blue-500
    val AccentDark       = Color(0xFF1D4ED8)   // blue-700
    val AccentMuted      = Color(0x1A3B82F6)   // blue 10%

    // Semantic
    val Connected        = Color(0xFF22C55E)   // green-500
    val ConnectedMuted   = Color(0x1F22C55E)
    val Disconnected     = Color(0xFFEF4444)   // red-500
    val DisconnectedMuted= Color(0x1AEF4444)

    val Warning          = Color(0xFFF97316)   // orange-500
    val WarningMuted     = Color(0x1FF97316)

    val Indigo           = Color(0xFF818CF8)   // indigo-400

    // Text
    val TextPrimary      = Color(0xFFF4F4F5)
    val TextSecondary    = Color(0xFFA1A1AA)
    val TextTertiary     = Color(0xFF71717A)

    // Temperature scale
    val TempHot          = Color(0xFFEF4444)
    val TempWarm         = Color(0xFFF97316)
    val TempMid          = Color(0xFF3B82F6)
    val TempCold         = Color(0xFF818CF8)
}

// ─── Material3 Color Scheme ───────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = SondaColors.Accent,
    onPrimary          = Color.White,
    primaryContainer   = SondaColors.AccentMuted,
    onPrimaryContainer = SondaColors.Accent,

    secondary          = SondaColors.Indigo,
    onSecondary        = Color.White,

    background         = SondaColors.Background,
    onBackground       = SondaColors.TextPrimary,

    surface            = SondaColors.Surface,
    onSurface          = SondaColors.TextPrimary,
    surfaceVariant     = SondaColors.SurfaceVariant,
    onSurfaceVariant   = SondaColors.TextSecondary,
    surfaceContainer   = SondaColors.SurfaceContainer,

    outline            = SondaColors.OutlineSubtle,
    outlineVariant     = SondaColors.OutlineMuted,

    error              = SondaColors.Disconnected,
    onError            = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary            = SondaColors.AccentDark,
    onPrimary          = Color.White,
    background         = Color(0xFFF4F4F5),
    surface            = Color.White,
    onSurface          = Color(0xFF18181B),
    surfaceVariant     = Color(0xFFF1F1F3),
    outline            = Color(0x1A000000),
)

// ─── Extended tokens ──────────────────────────────────────────────────────────

data class SondaExtendedColors(
    val connected: Color,
    val connectedMuted: Color,
    val disconnected: Color,
    val disconnectedMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val textTertiary: Color,
    val surfaceContainer: Color,
    val outlineSubtle: Color,
    val tempHot: Color,
    val tempWarm: Color,
    val tempMid: Color,
    val tempCold: Color,
)

val LocalSondaColors = staticCompositionLocalOf {
    SondaExtendedColors(
        connected          = SondaColors.Connected,
        connectedMuted     = SondaColors.ConnectedMuted,
        disconnected       = SondaColors.Disconnected,
        disconnectedMuted  = SondaColors.DisconnectedMuted,
        warning            = SondaColors.Warning,
        warningMuted       = SondaColors.WarningMuted,
        textTertiary       = SondaColors.TextTertiary,
        surfaceContainer   = SondaColors.SurfaceContainer,
        outlineSubtle      = SondaColors.OutlineSubtle,
        tempHot            = SondaColors.TempHot,
        tempWarm           = SondaColors.TempWarm,
        tempMid            = SondaColors.TempMid,
        tempCold           = SondaColors.TempCold,
    )
}

// ─── Spacing tokens ───────────────────────────────────────────────────────────

data class SondaSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalSondaSpacing = staticCompositionLocalOf { SondaSpacing() }

// ─── Typography ───────────────────────────────────────────────────────────────

// If you add a custom font (e.g. DMSans), declare it here:
// val DMSans = FontFamily(
//     Font(R.font.dmsans_regular, FontWeight.Normal),
//     Font(R.font.dmsans_medium,  FontWeight.Medium),
//     Font(R.font.dmsans_bold,    FontWeight.Bold),
// )

// ─── Theme entry point ────────────────────────────────────────────────────────

@Composable
fun SondaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSondaColors  provides LocalSondaColors.current,
        LocalSondaSpacing provides SondaSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content,
        )
    }
}

// ─── Convenience accessors ────────────────────────────────────────────────────

object SondaTheme {
    val colors: SondaExtendedColors
        @Composable get() = LocalSondaColors.current
    val spacing: SondaSpacing
        @Composable get() = LocalSondaSpacing.current
}