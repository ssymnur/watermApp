package com.seymanur.watermapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = lightColorScheme(
    primary = OrangeMain,
    onPrimary = Color.White,

    secondary = BlueMain,
    onSecondary = Color.White,

    background = Background,
    onBackground = Color.Black,

    surface = CardWhite
)

@Composable
fun WaterAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = Typography,
        content = content
    )
}
