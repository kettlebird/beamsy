package io.github.jqssun.airplay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.get

// "Start Beamsy when the TV turns on": the TV shows up on iPhones without opening the app first
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        if (!prefs[Prefs.BOOT_AUTO_START]) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, AirPlayService::class.java).setAction(AirPlayService.ACTION_START_SERVER)
        )
    }
}
