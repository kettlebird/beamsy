package io.github.jqssun.airplay

import android.content.Context

// TODO(Task 4): replaced by the real check in Unlock.kt; delete this stub then
@Suppress("UNUSED_PARAMETER")
fun isUnlocked(context: Context): Boolean = false

// Fit (w, h) inside 1280x720 (free) or 1920x1080 (unlocked), swapped for portrait. Integer math only: floats turn 1280 into 1279.
fun capForTier(w: Int, h: Int, unlocked: Boolean): Pair<Int, Int> {
    val (long, short) = if (unlocked) 1920 to 1080 else 1280 to 720
    val (boxW, boxH) = if (h > w) short to long else long to short
    val (cw, ch) = when {
        w <= boxW && h <= boxH -> w to h
        w * boxH >= h * boxW -> boxW to h * boxW / w
        else -> w * boxH / h to boxH
    }
    return (cw and 1.inv()) to (ch and 1.inv())
}
