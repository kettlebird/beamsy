package io.github.jqssun.airplay

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display

// physical panel resolution
fun Context.realDisplaySize(): Pair<Int, Int> {
    val mode = getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)?.mode
    return if (mode != null) mode.physicalWidth to mode.physicalHeight
    else resources.displayMetrics.let { it.widthPixels to it.heightPixels }
}

// the TV's user-visible name; Build.MODEL if unset
fun Context.deviceName(): String =
    Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() } ?: Build.MODEL

// advertised name: user override, else the TV's own name
fun Context.tvName(prefs: SharedPreferences): String = prefs[Prefs.SERVER_NAME].ifBlank { deviceName() }
