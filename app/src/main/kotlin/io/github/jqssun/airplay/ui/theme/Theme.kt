package io.github.jqssun.airplay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.jqssun.airplay.R

// Open Sauce Sans (SIL OFL, licence in assets/OpenSauceSans-OFL.txt)
private val OpenSauce = FontFamily(
    Font(R.font.open_sauce_sans_regular),
    Font(R.font.open_sauce_sans_medium, FontWeight.Medium),
    Font(R.font.open_sauce_sans_semibold, FontWeight.SemiBold),
    Font(R.font.open_sauce_sans_bold, FontWeight.Bold)
)

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = OpenSauce),
        displayMedium = displayMedium.copy(fontFamily = OpenSauce),
        displaySmall = displaySmall.copy(fontFamily = OpenSauce),
        headlineLarge = headlineLarge.copy(fontFamily = OpenSauce),
        headlineMedium = headlineMedium.copy(fontFamily = OpenSauce),
        headlineSmall = headlineSmall.copy(fontFamily = OpenSauce),
        titleLarge = titleLarge.copy(fontFamily = OpenSauce),
        titleMedium = titleMedium.copy(fontFamily = OpenSauce),
        titleSmall = titleSmall.copy(fontFamily = OpenSauce),
        bodyLarge = bodyLarge.copy(fontFamily = OpenSauce),
        bodyMedium = bodyMedium.copy(fontFamily = OpenSauce),
        bodySmall = bodySmall.copy(fontFamily = OpenSauce),
        labelLarge = labelLarge.copy(fontFamily = OpenSauce),
        labelMedium = labelMedium.copy(fontFamily = OpenSauce),
        labelSmall = labelSmall.copy(fontFamily = OpenSauce)
    )
}

@Composable
fun AirPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
