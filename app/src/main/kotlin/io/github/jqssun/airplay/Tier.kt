package io.github.jqssun.airplay

// Scale (w, h) down to fit maxW x maxH, keeping aspect, never upscaling. Integer math only: floats turn 1280 into 1279.
fun fitWithin(w: Int, h: Int, maxW: Int, maxH: Int): Pair<Int, Int> {
    val (cw, ch) = when {
        w <= maxW && h <= maxH -> w to h
        w * maxH >= h * maxW -> maxW to h * maxW / w
        else -> w * maxH / h to maxH
    }
    return (cw and 1.inv()) to (ch and 1.inv())
}

const val FREE_FPS = 30

// Free mirrors at up to 30 fps, unlimited at the full rate (60 by default).
fun fpsForTier(maxFps: Int, unlocked: Boolean): Int = if (unlocked) maxFps else minOf(maxFps, FREE_FPS)

// Fit inside 1280x720 (free) or 1920x1080 (unlocked), swapped for portrait.
fun capForTier(w: Int, h: Int, unlocked: Boolean): Pair<Int, Int> {
    val (long, short) = if (unlocked) 1920 to 1080 else 1280 to 720
    return if (h > w) fitWithin(w, h, short, long) else fitWithin(w, h, long, short)
}
