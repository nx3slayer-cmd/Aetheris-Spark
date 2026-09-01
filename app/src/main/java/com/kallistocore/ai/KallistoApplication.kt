package com.kallistocore.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kallistocore.ai.data.db.KallistoDatabase

class KallistoApplication : Application() {

    companion object {
        const val OVERLAY_CHANNEL_ID = "kallisto_overlay_channel"
        const val TTS_CHANNEL_ID = "kallisto_tts_channel"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Eagerly initialize SQLite / Room Database with WAL mode
        KallistoDatabase.getInstance(this)

        // 2. Initialize Android Notification Channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for Floating Overlay Companion
            val overlayChannel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "Kallisto Floating Companion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating companion overlay active over other apps"
                setShowBadge(false)
            }

            // Channel for Kokoro TTS background voice playback
            val ttsChannel = NotificationChannel(
                TTS_CHANNEL_ID,
                "Kokoro Neural Voice Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice synthesis playback"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(overlayChannel, ttsChannel))
        }
    }
}
