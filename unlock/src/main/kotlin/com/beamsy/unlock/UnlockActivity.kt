package com.beamsy.unlock

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button

private const val MAIN_PACKAGE = "com.beamsy.app"

class UnlockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock)
        findViewById<Button>(R.id.open_main).apply {
            setOnClickListener { openMain() }
            requestFocus()
        }
    }

    private fun openMain() {
        val launch = packageManager.getLeanbackLaunchIntentForPackage(MAIN_PACKAGE)
            ?: packageManager.getLaunchIntentForPackage(MAIN_PACKAGE)
        if (launch != null) return startActivity(launch)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$MAIN_PACKAGE")))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$MAIN_PACKAGE")))
        }
    }
}
