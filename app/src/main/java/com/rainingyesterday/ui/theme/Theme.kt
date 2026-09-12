package com.rainingyesterday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Apricot,
    onPrimary = Paper,
    primaryContainer = Marigold,
    onPrimaryContainer = WarmInk,
    secondary = Sage,
    onSecondary = WarmInk,
    secondaryContainer = Cream,
    onSecondaryContainer = WarmInk,
    tertiary = Terracotta,
    onTertiary = Paper,
    background = Paper,
    onBackground = WarmInk,
    surface = Cream,
    onSurface = WarmInk,
    surfaceVariant = Linen,
    onSurfaceVariant = WarmBrown,
    outline = WarmBrown,
)

// 暗色 = 06 §7「深层」基调：暖光点缀（金盏黄）+ 雾蓝冷调，幽美静谧而非惊悚
private val DarkColors = darkColorScheme(
    primary = Marigold,
    onPrimary = WarmInk,
    primaryContainer = Terracotta,
    onPrimaryContainer = Paper,
    secondary = MistBlue,
    onSecondary = WarmInk,
    secondaryContainer = DeepSurface,
    onSecondaryContainer = Cream,
    tertiary = Blush,
    onTertiary = WarmInk,
    background = DeepBackground,
    onBackground = Cream,
    surface = DeepSurface,
    onSurface = Cream,
    surfaceVariant = DeepHaze,
    onSurfaceVariant = Linen,
    outline = Linen,
)

/** 《昨夜有雨》应用主题：表层温暖治愈、夜间自动切「深层」冷调变体。 */
@Composable
fun RainingYesterdayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}