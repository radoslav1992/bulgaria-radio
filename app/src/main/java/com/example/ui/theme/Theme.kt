package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VintageRadioColorScheme = darkColorScheme(
    primary = AmberGlow,
    onPrimary = VinylBlack,
    primaryContainer = MahoganyPanel,
    onPrimaryContainer = CreamWhite,
    secondary = BrassGold,
    onSecondary = VinylBlack,
    secondaryContainer = WalnutMedium,
    onSecondaryContainer = ParchmentText,
    tertiary = VintageGreen,
    onTertiary = Color.White,
    background = WalnutDark,
    onBackground = CreamWhite,
    surface = WalnutMedium,
    onSurface = ParchmentText,
    surfaceVariant = WalnutLight,
    onSurfaceVariant = FadedLabel,
    error = RadioRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VintageRadioColorScheme,
        typography = Typography,
        content = content
    )
}
