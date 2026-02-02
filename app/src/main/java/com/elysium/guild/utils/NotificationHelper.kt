package com.elysium.guild.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elysium.guild.MainActivity
import com.elysium.guild.R

class NotificationHelper(private val context: Context) {

    companion object {
        // Increment the ID version (e.g., _v3) whenever you change the sound file
        // because Android lock-in channel settings once created.
        private const val CHANNEL_ID = "boss_spawn_channel_v3"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Boss & Event Alerts"
            val descriptionText = "Notifications for boss spawns and guild events"
            val importance = NotificationManager.IMPORTANCE_HIGH

            // Link to the sound file in res/raw/boss_spawn.mp3
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.boss_spawn}")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
                // Set the sound for the channel
                setSound(soundUri, audioAttributes)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBossNotification(bossName: String, minutesRemaining: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, bossName.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.boss_spawn}")

        val title = "$bossName in $minutesRemaining mins!"
        val message = "Prepare for the kill! Boss spawns in $minutesRemaining minutes."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri) // Set sound for older Android versions
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(bossName.hashCode(), builder.build())
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show notification", e)
        }
    }
}
