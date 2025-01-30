package com.mrcattington.ipcamscreensaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrcattington.ipcamscreensaver.ui.theme.IPCamScreensaverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IPCamScreensaverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                startActivity(Intent(this@MainActivity, DreamSettingsActivity::class.java))
                            }
                        ) {
                            Text("Configure RTSP URL")
                        }

                        Button(
                            onClick = {
                                val intent = Intent()
                                intent.action = Intent.ACTION_MAIN
                                intent.setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                                startActivity(intent)
                                finish()
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Start Screensaver")
                        }
                    }
                }
            }
        }
    }
}