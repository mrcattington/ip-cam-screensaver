package com.mrcattington.ipcamscreensaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if RTSP URL is configured
        val rtspUrl = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("rtsp_url", "")

        if (rtspUrl.isNullOrEmpty()) {
            // If no URL is configured, open settings
            startActivity(Intent(this, DreamSettingsActivity::class.java))
            finish()
        } else {
            // If URL is configured, start screensaver directly
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
            }
            startActivity(intent)
            finish()
        }
    }
}