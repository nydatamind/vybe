package com.example.vybe.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Colors (Electric Cyan / Aqua Neon Theme matching user's design)
val VybeEmerald = Color(0xFF00B7D8)           // Primary vibrant cyan / aqua (matches buttons and FAB)
val VybeEmeraldBright = Color(0xFF00E5FF)     // Electric neon cyan (matches glowing icons & active tabs)
val VybeEmeraldDark = Color(0xFF007A99)       // Deep rich aqua
val VybeCyan = Color(0xFF00E5FF)
val VybePurple = Color(0xFF8E24AA)
val VybePurpleLight = Color(0xFFBA68C8)
val VybePink = Color(0xFFFF4081)
val VybeOrange = Color(0xFFFF6D00)
val NeonCyan = VybeCyan
val NeonPurple = VybePurple

// Dark Theme Surfaces (Sleek Spotify-Grade Hierarchy)
val DarkBackground = Color(0xFF0D0E12)
val DarkSurface = Color(0xFF14161E)
val DarkSurfaceElevated = Color(0xFF1C1F2B)
val DarkSurfaceGlass = Color(0xCC181A24)
val DarkCard = Color(0xFF222636)
val DarkCardBorder = Color(0x22FFFFFF)

// Light Theme Surfaces
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEEF0F5)
val LightSurfaceGlass = Color(0xDDF8F9FC)
val LightCard = Color(0xFFFFFFFF)
val LightCardBorder = Color(0x11000000)

// Text Colors
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFB0B3C1)
val TextMutedDark = Color(0xFF6E7387)

val TextPrimaryLight = Color(0xFF12141A)
val TextSecondaryLight = Color(0xFF555B6E)
val TextMutedLight = Color(0xFF9197A6)

// Gradients
val VybeGradientEmerald = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0096C7), Color(0xFF00B7D8), Color(0xFF00E5FF))
)

val VybeGradientPurple = Brush.linearGradient(
    colors = listOf(Color(0xFFBA68C8), Color(0xFF8E24AA), Color(0xFF512DA8))
)

val VybeGradientDarkGlass = Brush.verticalGradient(
    colors = listOf(Color(0xEE1A1D29), Color(0xFA0D0E12))
)

val VybeCardGlow = Brush.radialGradient(
    colors = listOf(Color(0x4000E5FF), Color(0x0000E5FF))
)

val VybeGlassBorder = Brush.verticalGradient(
    colors = listOf(Color(0x38FFFFFF), Color(0x0AFFFFFF))
)

val VybeGlowCyanBrush = Brush.horizontalGradient(
    colors = listOf(VybeEmerald, VybeEmeraldBright)
)

val VybeCardBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1F2333), Color(0xFF141724))
)

val VybeAccentGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF00B7D8), Color(0xFF00FF88))
)
