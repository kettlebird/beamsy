package io.github.jqssun.airplay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

const val UNLOCK_PACKAGE = "com.beamsy.unlock"

// Full HD is unlocked by installing the paid key app, which must be signed with the same key as this app.
fun isUnlocked(context: Context): Boolean = try {
    val pm = context.packageManager
    val own = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
    own != null && pm.hasSigningCertificate(UNLOCK_PACKAGE, own, PackageManager.CERT_INPUT_RAW_X509)
} catch (_: PackageManager.NameNotFoundException) {
    false
}

fun openUnlockListing(context: Context) {
    fun view(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(view("market://details?id=$UNLOCK_PACKAGE"))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(view("https://play.google.com/store/apps/details?id=$UNLOCK_PACKAGE"))
    }
}
