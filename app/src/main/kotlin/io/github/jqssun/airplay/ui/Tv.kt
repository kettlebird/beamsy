package io.github.jqssun.airplay.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// the TV's own link to the router: null when offline or unknown, 0 for Ethernet, 1..4 Wi-Fi bars
@Suppress("DEPRECATION") // connectionInfo: the only RSSI source on API 28 and some TV drivers
fun Context.linkBars(): Int? {
    val cm = getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return null
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return 0
    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
    var rssi = if (Build.VERSION.SDK_INT >= 29) caps.signalStrength else NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED
    if (rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) rssi = getSystemService(WifiManager::class.java).connectionInfo.rssi
    return when {
        rssi <= -127 || rssi >= 0 -> null // driver didn't report one
        rssi >= -55 -> 4
        rssi >= -66 -> 3
        rssi >= -77 -> 2
        else -> 1
    }
}

fun Context.isOnline(): Boolean = getSystemService(ConnectivityManager::class.java).activeNetwork != null

fun Context.isTvDevice(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType ==
        Configuration.UI_MODE_TYPE_TELEVISION

@Composable
fun isTv(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) { ctx.isTvDevice() }
}

fun Modifier.dpadFocus(shape: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    onFocusChanged { focused = it.isFocused }
        .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
}

fun Modifier.dpadAdjust(onLeft: () -> Unit, onRight: () -> Unit): Modifier = composed {
    val focus = LocalFocusManager.current
    onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (e.key) {
            Key.DirectionLeft -> { onLeft(); true }
            Key.DirectionRight -> { onRight(); true }
            // slider
            Key.DirectionUp -> { focus.moveFocus(FocusDirection.Up); true }
            Key.DirectionDown -> { focus.moveFocus(FocusDirection.Down); true }
            else -> false
        }
    }
}

// The UI is laid out for a 960×540 dp TV. Other screens (phones, tablets) get it scaled to fit and centred;
// on TVs the scale is 1. Overlays re-apply `density` inside their own windows.
@Composable
fun TvStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth / STAGE_WIDTH, maxHeight / STAGE_HEIGHT)
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale)) {
            Box(Modifier.size(STAGE_WIDTH, STAGE_HEIGHT), content = content)
        }
    }
}

val STAGE_WIDTH = 960.dp
val STAGE_HEIGHT = 540.dp
