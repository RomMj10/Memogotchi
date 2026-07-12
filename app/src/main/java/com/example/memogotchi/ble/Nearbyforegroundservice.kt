package com.example.memogotchi.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.memogotchi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NearbyForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "goal_buddy_nearby_active"
        const val NOTIFICATION_ID = 8801
        private const val TOKEN_CHECK_INTERVAL_MS = 60_000L
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)

    private lateinit var advertiser: BleAdvertiser
    private lateinit var scanner: BleScanner

    override fun onCreate() {
        super.onCreate()
        advertiser = BleAdvertiser(this)
        scanner = BleScanner(this, serviceScope) { matchId ->
            // Server already sends the FCM push on match (see Step 5);
            // nothing further needed here for now.
        }
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scanner.start(BleRadioMode.LOW_LATENCY)

        serviceScope.launch {
            var lastToken: String? = null
            while (true) {
                val token = NearbyTokenManager.ensureFreshToken()
                if (token != null && token != lastToken) {
                    advertiser.start(token, BleRadioMode.LOW_LATENCY)
                    lastToken = token
                }
                delay(TOKEN_CHECK_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        advertiser.stop()
        scanner.stop()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Goal Buddy — Nearby Scanning",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Looking for nearby memos…")
            .setContentText("Goal Buddy is actively scanning")
            .setSmallIcon(R.drawable.memogotchi_vector)
            .setOngoing(true)
            .build()
    }
}