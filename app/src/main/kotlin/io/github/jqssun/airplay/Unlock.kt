package io.github.jqssun.airplay

import android.content.Context
import android.content.pm.PackageManager

const val UNLOCK_PACKAGE = "com.beamsy.unlock"

// Full HD: the Play purchase (FullHd), or for builds outside Play the paid key app signed with this app's key.
fun isUnlocked(context: Context): Boolean =
    context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)[Prefs.FULL_HD] || hasUnlockApp(context)

private fun hasUnlockApp(context: Context): Boolean = try {
    val pm = context.packageManager
    val own = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
    own != null && pm.hasSigningCertificate(UNLOCK_PACKAGE, own, PackageManager.CERT_INPUT_RAW_X509)
} catch (_: PackageManager.NameNotFoundException) {
    false
}
