package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PeachPrimaryDark,
    secondary = OrangeSecondaryDark,
    tertiary = AmberTertiaryDark,
    background = DarkCocoaBackground,
    surface = DarkCocoaSurface,
    onPrimary = SoftCharcoal,
    onSecondary = SoftCharcoal,
    onTertiary = SoftCharcoal,
    onBackground = WarmBackground,
    onSurface = WarmBackground
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PeachPrimary,
    secondary = OrangeSecondary,
    tertiary = AmberTertiary,
    background = WarmBackground,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = SoftCharcoal,
    onBackground = SoftCharcoal,
    onSurface = SoftCharcoal
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
