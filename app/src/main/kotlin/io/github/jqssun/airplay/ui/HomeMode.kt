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

// default advertised name: the device name if it fits, else the model, else the model cut down to fit
fun safeTvName(raw: String, model: String): String {
    raw.trim().takeIf(::isValidTvName)?.let { return it }
    model.trim().takeIf(::isValidTvName)?.let { return it }
    val sb = StringBuilder()
    for (cp in model.trim().ifBlank { raw.trim() }.codePoints()) {
        if (!isValidTvName(sb.toString() + String(Character.toChars(cp)))) break
        sb.appendCodePoint(cp)
    }
    return sb.toString()
}
