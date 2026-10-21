package io.github.jqssun.airplay

import android.content.Context
import android.content.SharedPreferences

enum class Plan { TRIAL, FREE, UNLIMITED }

// Plans (design/beamsy-ui.html):
//   trial     – the first 60 minutes of mirroring after install: Full HD, 60 fps, no session limit
//   free      – afterwards: 720p, 30 fps, each session stops after 15 minutes; reconnecting starts a new one
//   unlimited – the one-time purchase (or the key app): Full HD, 60 fps, no limits
// Only the screen counts (mirroring, video links): audio-only streaming is never limited.
// shortcut: the trial is counted on the TV, so clearing app data restarts it; move to a server if that gets abused
object FreeTime {
    const val TRIAL_MINUTES = 60
    const val TRIAL_SECONDS = TRIAL_MINUTES * 60
    const val SESSION_MINUTES = 15
    const val SESSION_SECONDS = SESSION_MINUTES * 60
    const val WARN_SECONDS = 2 * 60

    fun trialLeft(usedSeconds: Int): Int = (TRIAL_SECONDS - usedSeconds).coerceAtLeast(0)

    fun plan(unlimited: Boolean, trialLeftSeconds: Int): Plan = when {
        unlimited -> Plan.UNLIMITED
        trialLeftSeconds > 0 -> Plan.TRIAL
        else -> Plan.FREE
    }

    fun trialLeft(prefs: SharedPreferences): Int = trialLeft(prefs[Prefs.TRIAL_USED])

    fun addTrial(prefs: SharedPreferences, seconds: Int) {
        Prefs.TRIAL_USED.write(prefs, (prefs[Prefs.TRIAL_USED] + seconds).coerceAtMost(TRIAL_SECONDS))
    }

    fun plan(context: Context, prefs: SharedPreferences): Plan = plan(isUnlocked(context), trialLeft(prefs))

    // minutes as people read them: 61 s left is "2 minutes", never "1" while more than a minute remains
    fun minutes(seconds: Int): Int = (seconds + 59) / 60
}
