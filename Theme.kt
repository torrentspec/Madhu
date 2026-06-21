package com.foss.madhu.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette

// ──────────────────────────────────────────────────────────────────────────────
// Static AMOLED dark colour scheme
// ──────────────────────────────────────────────────────────────────────────────

private val AmoledDarkColorScheme = darkColorScheme(
    primary             = Amoled_Primary,
    onPrimary           = Amoled_OnPrimary,
    primaryContainer    = Amoled_PrimaryContain,
    onPrimaryContainer  = Amoled_OnPriContain,
    secondary           = Amoled_Secondary,
    onSecondary         = Amoled_OnSecondary,
    secondaryContainer  = Amoled_SecContain,
    onSecondaryContainer= Amoled_OnSecContain,
    tertiary            = Amoled_Tertiary,
    onTertiary          = Amoled_OnTertiary,
    error               = Amoled_Error,
    onError             = Amoled_OnError,
    background          = Amoled_Background,
    onBackground        = Amoled_OnBackground,
    surface             = Amoled_Surface,
    onSurface           = Amoled_OnSurface,
    surfaceVariant      = Amoled_SurfaceVar,
    onSurfaceVariant    = Amoled_OnSurfaceVar,
    outline             = Amoled_Outline,
    surfaceTint         = Amoled_Primary
)

// ──────────────────────────────────────────────────────────────────────────────
// Composition-local for album art-driven dynamic accent
// ──────────────────────────────────────────────────────────────────────────────

val LocalAlbumPalette = staticCompositionLocalOf { AlbumPalette() }

// ──────────────────────────────────────────────────────────────────────────────
// Album palette extractor
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Extracts a [AlbumPalette] from [bitmap] on the calling dispatcher (should be
 * called from an IO or Default coroutine).  Falls back gracefully if Palette
 * generation fails.
 */
suspend fun extractAlbumPalette(bitmap: Bitmap): AlbumPalette {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            val palette = Palette.Builder(bitmap)
                .maximumColorCount(16)
                .generate()

            val fallbackArgb = Amoled_Primary.toArgb()
            AlbumPalette(
                vibrant     = Color(palette.getVibrantColor(fallbackArgb)),
                darkVibrant = Color(palette.getDarkVibrantColor(Amoled_PrimaryContain.toArgb())),
                muted       = Color(palette.getMutedColor(Amoled_SurfaceVar.toArgb())),
                lightMuted  = Color(palette.getLightMutedColor(Amoled_OnSurfaceVar.toArgb()))
            )
        } catch (e: Exception) {
            AlbumPalette()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// MadhuTheme
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Root Material 3 theme for Madhu.
 *
 * [isAmoled]      — uses the pure-black `#000000` background scheme.
 * [isDynamicColor]— on Android 12+, uses Material You wallpaper-extracted
 *                   colours; on older devices falls back to [AmoledDarkColorScheme].
 * [albumPalette]  — the Palette-API colours extracted from the current album art;
 *                   exposed via [LocalAlbumPalette] for the Now Playing screen.
 */
@Composable
fun MadhuTheme(
    isAmoled:       Boolean     = true,
    isDynamicColor: Boolean     = true,
    albumPalette:   AlbumPalette = AlbumPalette(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isAmoled) {
                // Derive dynamic scheme from wallpaper, then force background to pure black.
                dynamicDarkColorScheme(context).copy(
                    background   = Amoled_Background,
                    surface      = Amoled_Surface,
                    surfaceVariant = Amoled_SurfaceVar
                )
            } else {
                dynamicDarkColorScheme(context)
            }
        }
        isAmoled -> AmoledDarkColorScheme
        else     -> darkColorScheme()   // generic dark fallback
    }

    // Edge-to-edge rendering: status bar and nav bar bleed into our pure-black canvas.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor  = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalAlbumPalette provides albumPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = MadhuTypography,
            content     = content
        )
    }
}
