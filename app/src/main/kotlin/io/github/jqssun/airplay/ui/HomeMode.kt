package io.github.jqssun.airplay.ui

enum class HomeMode { STEPS, AMBIENT, AUDIO, MIRROR }

const val AMBIENT_AFTER_MS = 60_000L

fun homeMode(mirroring: Boolean, audioOnly: Boolean, idleMs: Long): HomeMode = when {
    mirroring -> HomeMode.MIRROR
    audioOnly -> HomeMode.AUDIO
    idleMs >= AMBIENT_AFTER_MS -> HomeMode.AMBIENT
    else -> HomeMode.STEPS
}

// 50 bytes leaves room for UxPlay's "MAC@" prefix inside mDNS's 63-byte label
fun isValidTvName(name: String): Boolean {
    val t = name.trim()
    return t.codePointCount(0, t.length) in 1..40 && t.toByteArray(Charsets.UTF_8).size <= 50
}
