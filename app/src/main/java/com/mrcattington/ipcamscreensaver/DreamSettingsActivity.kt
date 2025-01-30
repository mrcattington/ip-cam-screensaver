package com.mrcattington.ipcamscreensaver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.Button
import androidx.preference.PreferenceManager

class DreamSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dream_settings)

        val urlEditText = findViewById<EditText>(R.id.rtspUrlEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Load existing URL if any
        urlEditText.setText(
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString("rtsp_url", "")
        )

        saveButton.setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("rtsp_url", urlEditText.text.toString())
                .apply()
            finish()
        }
    }
}