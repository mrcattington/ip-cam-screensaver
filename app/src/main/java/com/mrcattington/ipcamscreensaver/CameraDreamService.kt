package com.mrcattington.ipcamscreensaver

import android.service.dreams.DreamService
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager

@UnstableApi
class CameraDreamService : DreamService() {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var muteButton: ImageButton? = null
    private var isMuted = true
    private val tag = "CameraDreamService"
    private var healthCheckRunnable: Runnable? = null
    private var streamRefreshRunnable: Runnable? = null

    private fun getStreamUrl(): String {
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getString("rtsp_url", "") ?: ""
    }

    override fun onAttachedToWindow() {
        try {
            super.onAttachedToWindow()
            Log.d(tag, "onAttachedToWindow called")

            isFullscreen = true
            isInteractive = true  // Set to true to enable touch events

            // Create the player view with proper aspect ratio handling
            playerView = PlayerView(this).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            Log.d(tag, "PlayerView created")

            // Create mute button
            muteButton = ImageButton(this).apply {
                setImageResource(R.drawable.ic_volume_off)
                background = null
                alpha = 0.7f
                setPadding(40, 40, 40, 40)
            }

            // Create a frame layout to hold both the player view and mute button
            val layout = FrameLayout(this).apply {
                // Add player view
                addView(playerView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))

                // Add mute button with positioning
                val buttonParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    marginStart = 50
                    bottomMargin = 50
                }
                addView(muteButton, buttonParams)

                // Add touch listener to the root layout
                setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Get the raw touch coordinates
                            val touchX = event.rawX
                            val touchY = event.rawY

                            // Check if touch is within mute button bounds
                            muteButton?.let { button ->
                                val buttonLocation = IntArray(2)
                                button.getLocationOnScreen(buttonLocation)

                                val withinX = touchX >= buttonLocation[0] &&
                                        touchX <= (buttonLocation[0] + button.width)
                                val withinY = touchY >= buttonLocation[1] &&
                                        touchY <= (buttonLocation[1] + button.height)

                                if (withinX && withinY) {
                                    view.performClick()
                                    return@setOnTouchListener false  // Let button handle its own click
                                }
                            }

                            // If we're here, touch was outside mute button - finish dream
                            view.performClick()
                            finish()
                            true
                        }
                        else -> false
                    }
                }
            }
            setContentView(layout)
            Log.d(tag, "Layout set")

            // Set up mute button click listener
            muteButton?.setOnClickListener {
                player?.let { exoPlayer ->
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                    muteButton?.setImageResource(
                        if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
                    )
                }
            }
            player = ExoPlayer.Builder(this)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                .build().apply {
                    playerView?.player = this
                    repeatMode = Player.REPEAT_MODE_ALL
                    playWhenReady = true
                    // Start un-muted
                    volume = 0f
                    Log.d(tag, "Initial player volume set to: $volume")

                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(tag, "Player error: ${error.message}", error)
                            // Wait briefly then attempt to reconnect
                            playerView?.postDelayed({
                                player?.let { exoPlayer ->
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                            }, 5000) // 5 second delay before retry
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val stateText = when (playbackState) {
                                Player.STATE_IDLE -> "STATE_IDLE"
                                Player.STATE_BUFFERING -> "STATE_BUFFERING"
                                Player.STATE_READY -> "STATE_READY"
                                Player.STATE_ENDED -> "STATE_ENDED"
                                else -> "UNKNOWN"
                            }
                            Log.d(tag, "Playback state changed to: $stateText")
                            if (playbackState == Player.STATE_READY) {
                                val audioTrackCount = this@apply.audioFormat?.let { 1 } ?: 0
                                Log.d(tag, "Number of audio tracks: $audioTrackCount")
                                Log.d(tag, "Current volume: ${this@apply.volume}")
                                Log.d(tag, "Audio session ID: ${this@apply.audioSessionId}")
                            }
                        }
                    })

                    val currentUrl = getStreamUrl()
                    if (currentUrl.isNotEmpty()) {
                        val rtspMediaSource = RtspMediaSource.Factory()
                            .setForceUseRtpTcp(true)  // Use TCP for better reliability
                            .setTimeoutMs(30000)
                            .setDebugLoggingEnabled(true)
                            .createMediaSource(MediaItem.fromUri(currentUrl))

                        Log.d(tag, "Setting up RTSP media source with audio enabled")
                        setMediaSource(rtspMediaSource)
                        prepare()
                        Log.d(tag, "Player prepared")
                    } else {
                        Log.e(tag, "No RTSP URL configured")
                    }
                }

            healthCheckRunnable = Runnable {
                player?.let { exoPlayer ->
                    if (exoPlayer.playbackState == Player.STATE_READY &&
                        exoPlayer.playWhenReady &&
                        !exoPlayer.isPlaying) {
                        // Stream appears frozen, restart it
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
                healthCheckRunnable?.let {
                    playerView?.postDelayed(it, 30000) // Check every 30 seconds
                }
            }.also { runnable ->
                playerView?.postDelayed(runnable, 30000)
            }

            streamRefreshRunnable = Runnable {
                player?.let { exoPlayer ->
                    // Reload the stream periodically
                    val currentPosition = exoPlayer.currentPosition
                    exoPlayer.prepare()
                    exoPlayer.seekTo(currentPosition)
                    exoPlayer.play()
                }
                streamRefreshRunnable?.let {
                    playerView?.postDelayed(it, 4 * 60 * 60 * 1000) // Refresh every 4 hours
                }
            }.also { runnable ->
                playerView?.postDelayed(runnable, 4 * 60 * 60 * 1000)
            }

        } catch (e: Exception) {
            Log.e(tag, "Error in onAttachedToWindow", e)
        }
    }

    override fun onDreamingStopped() {
        try {
            super.onDreamingStopped()
            Log.d(tag, "onDreamingStopped called")
            releasePlayer()
        } catch (e: Exception) {
            Log.e(tag, "Error in onDreamingStopped", e)
        }
    }

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
            Log.d(tag, "onDetachedFromWindow called")
            releasePlayer()
        } catch (e: Exception) {
            Log.e(tag, "Error in onDetachedFromWindow", e)
        }
    }

    private fun releasePlayer() {
        try {
            healthCheckRunnable?.let { runnable ->
                playerView?.removeCallbacks(runnable)
            }
            healthCheckRunnable = null

            streamRefreshRunnable?.let { runnable ->
                playerView?.removeCallbacks(runnable)
            }
            streamRefreshRunnable = null

            player?.let { exoPlayer ->
                exoPlayer.release()
                player = null
                Log.d(tag, "Player released")
            }
            playerView = null
            muteButton = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing player", e)
        }
    }
}