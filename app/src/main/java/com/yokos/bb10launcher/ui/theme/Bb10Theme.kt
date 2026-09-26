package com.yokos.bb10launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yokos.bb10launcher.R

object Bb10Colors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF1E1E1E)
    val SurfaceHigh = Color(0xFF2A2A2A)
    val Blue = Color(0xFF00A8DF)
    val Text = Color(0xFFF2F2F2)
    val TextDim = Color(0xFF9A9A9A)
    val Divider = Color(0xFF333333)

    /** Dims the wallpaper behind the home pages. */
    val Scrim = Color(0x66000000)
}

/** Source Sans 3 (SIL OFL) stands in for BB10's Slate Pro, which can't be redistributed. */
val SlateFamily = FontFamily(
    Font(R.font.source_sans3_light, FontWeight.Light),
    Font(R.font.source_sans3_regular, FontWeight.Normal),
    Font(R.font.source_sans3_semibold, FontWeight.SemiBold),
)

private val Bb10ColorScheme = darkColorScheme(
    primary = Bb10Colors.Blue,
    onPrimary = Color.White,
    secondary = Bb10Colors.Blue,
    background = Bb10Colors.Black,
    onBackground = Bb10Colors.Text,
    surface = Bb10Colors.Surface,
    onSurface = Bb10Colors.Text,
    surfaceVariant = Bb10Colors.SurfaceHigh,
    onSurfaceVariant = Bb10Colors.TextDim,
    surfaceContainerHigh = Bb10Colors.SurfaceHigh,
    outline = Bb10Colors.Divider,
)

private val Bb10Typography = Typography().run {
    fun TextStyle.slate() = copy(fontFamily = SlateFamily)
    Typography(
        displayLarge = displayLarge.slate().copy(fontWeight = FontWeight.Light),
        displayMedium = displayMedium.slate().copy(fontWeight = FontWeight.Light),
        displaySmall = displaySmall.slate().copy(fontWeight = FontWeight.Light),
        headlineLarge = headlineLarge.slate().copy(fontWeight = FontWeight.Light),
        headlineMedium = headlineMedium.slate().copy(fontWeight = FontWeight.Light),
        headlineSmall = headlineSmall.slate(),
        titleLarge = titleLarge.slate(),
        titleMedium = titleMedium.slate().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.slate().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.slate().copy(fontSize = 17.sp),
        bodyMedium = bodyMedium.slate().copy(fontSize = 15.sp),
        bodySmall = bodySmall.slate(),
        labelLarge = labelLarge.slate().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.slate(),
        labelSmall = labelSmall.slate(),
    )
}

@Composable
fun Bb10Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Bb10ColorScheme, typography = Bb10Typography, content = content)
}
