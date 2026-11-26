package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HydroGrowLightColors = lightColorScheme(
    primary = LeafGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = LeafGreenContainer,
    onPrimaryContainer = OnLeafGreenContainer,
    secondary = MintSecondary,
    onSecondary = Color.White,
    secondaryContainer = MintContainer,
    onSecondaryContainer = OnMintContainer,
    tertiary = OrangeHighlight,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight
)

private val HydroGrowDarkColors = darkColorScheme(
    primary = MintLight,
    onPrimary = OnLeafGreenContainer,
    primaryContainer = LeafGreenDeep,
    onPrimaryContainer = LeafGreenContainer,
    secondary = MintSecondary,
    onSecondary = Color.White,
    secondaryContainer = OnMintContainer,
    onSecondaryContainer = MintContainer,
    tertiary = AmberAccent,
    onTertiary = OnAmberContainer,
    tertiaryContainer = OnAmberContainer,
    onTertiaryContainer = AmberContainer,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = ErrorRed,
    onError = Color.White
)

private val HydroGrowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HydroGrowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HydroGrowDarkColors else HydroGrowLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HydroGrowShapes,
        content = content
    )
}
