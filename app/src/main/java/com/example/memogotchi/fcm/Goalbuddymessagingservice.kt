package com.example.memogotchi.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.memogotchi.MainActivity
import com.example.memogotchi.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GoalBuddyMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "goal_buddy_nearby"
        private const val NOTIFICATION_ID = 8802

        // NOTE: assumed to match this project's existing notification-channel
        // style (createNotificationChannel()/createGoalNotificationChannels()
        // in ui/page) — I don't have that file's exact contents, so this
        // creates its own channel independently. If those existing functions
        // already centralize channel creation, consider moving this channel
        // definition alongside them instead for consistency.
        fun createChannel(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Goal Buddy — Nearby Matches",
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"]
        if (type != "nearby_match") return

        val matchId = message.data["matchId"]
        val title = message.notification?.title ?: "Memo nearby!"
        val body = message.notification?.body ?: "I sense a memo nearby! Let's connect!"

        showNotification(title, body, matchId)
    }

    private fun showNotification(title: String, body: String, matchId: String?) {
        createChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deepLinkTarget", "standby")
            putExtra("matchId", matchId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.memogotchi_vector)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }
}