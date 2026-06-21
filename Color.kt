package com.foss.madhu.ui.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────────────
// AMOLED static palette
// ──────────────────────────────────────────────────────────────────────────────

/** Absolute true black for AMOLED displays — kills all pixels, zero power draw. */
val Amoled_Background    = Color(0xFF000000)
val Amoled_Surface       = Color(0xFF090909)
val Amoled_SurfaceVar    = Color(0xFF141414)
val Amoled_Outline       = Color(0xFF2C2C2C)

val Amoled_Primary       = Color(0xFFCB9AF7)  // Soft purple — Madhu's signature
val Amoled_OnPrimary     = Color(0xFF2A0054)
val Amoled_PrimaryContain= Color(0xFF3D0073)
val Amoled_OnPriContain  = Color(0xFFECDAFF)

val Amoled_Secondary     = Color(0xFFAFC8FF)  // Periwinkle accent
val Amoled_OnSecondary   = Color(0xFF003066)
val Amoled_SecContain    = Color(0xFF00468C)
val Amoled_OnSecContain  = Color(0xFFD8E3FF)

val Amoled_Tertiary      = Color(0xFFFFB3C6)  // Warm pink for favorites / hearts
val Amoled_OnTertiary    = Color(0xFF5C0029)

val Amoled_Error         = Color(0xFFFF7070)
val Amoled_OnError       = Color(0xFF690000)

val Amoled_OnBackground  = Color(0xFFEEEEEE)
val Amoled_OnSurface     = Color(0xFFDDDDDD)
val Amoled_OnSurfaceVar  = Color(0xFFAAAAAA)

// ──────────────────────────────────────────────────────────────────────────────
// Dynamic palette seed (used when Dynamic Color is disabled on older devices)
// ──────────────────────────────────────────────────────────────────────────────

val Madhu_Seed = Color(0xFFCB9AF7)

// ──────────────────────────────────────────────────────────────────────────────
// Album-art extracted dynamic colours (updated at runtime via Palette API)
// ──────────────────────────────────────────────────────────────────────────────

data class AlbumPalette(
    val vibrant:     Color = Amoled_Primary,
    val darkVibrant: Color = Amoled_PrimaryContain,
    val muted:       Color = Amoled_SurfaceVar,
    val lightMuted:  Color = Amoled_OnSurfaceVar
)
