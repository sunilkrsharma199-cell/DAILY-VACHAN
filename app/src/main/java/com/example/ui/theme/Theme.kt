package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DeepTerracotta,
    onPrimary = WarmIvoryCard,
    primaryContainer = SoftTerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = ForestOlive,
    onSecondary = WarmIvoryCard,
    secondaryContainer = OliveContainer,
    onSecondaryContainer = OnOliveContainer,
    tertiary = BurnishedGold,
    onTertiary = WarmIvoryCard,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = ParchmentBackground,
    onBackground = EspressoInk,
    surface = WarmIvoryCard,
    onSurface = EspressoInk,
    surfaceVariant = SandalwoodSurface,
    onSurfaceVariant = WarmUmber,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutline.copy(alpha = 0.55f),
    error = CrimsonAlert
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronGlow,
    onPrimary = EveningCharcoal,
    primaryContainer = TerracottaDarkContainer,
    onPrimaryContainer = OnTerracottaDarkContainer,
    secondary = SageMist,
    onSecondary = EveningCharcoal,
    secondaryContainer = OliveDarkContainer,
    onSecondaryContainer = OnOliveDarkContainer,
    tertiary = WarmGoldGlow,
    onTertiary = EveningCharcoal,
    tertiaryContainer = GoldDarkContainer,
    onTertiaryContainer = OnGoldDarkContainer,
    background = EveningCharcoal,
    onBackground = WarmCreamText,
    surface = CedarCard,
    onSurface = WarmCreamText,
    surfaceVariant = DeepCedarSurface,
    onSurfaceVariant = MutedSandText,
    outline = CedarOutline,
    outlineVariant = CedarOutline.copy(alpha = 0.6f)
)

val DailyVachanShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DailyVachanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DailyVachanTypography,
        shapes = DailyVachanShapes,
        content = content
    )
}
