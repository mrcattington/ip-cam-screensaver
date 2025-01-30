package com.mrcattington.ipcamscreensaver

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.preference.PreferenceManager

class DreamSettingsActivity : Activity() {  // Changed from AppCompatActivity

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
            // Save the URL
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("rtsp_url", urlEditText.text.toString())
                .apply()
            finish()
        }
    }
}