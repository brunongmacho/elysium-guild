package com.elysium.guild.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context,
    private val preferenceManager: PreferenceManager
) {

    companion object {
        private const val BASE_CHANNEL_ID = "boss_spawn_channel"
    }

    init {
        createNotificationChannel()
    }

    private fun getChannelId(): String {
        val soundName = preferenceManager.notificationSound.value
        return "${BASE_CHANNEL_ID}_$soundName"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundName = preferenceManager.notificationSound.value
            val channelId = getChannelId()
            
            val name = "Boss & Event Alerts"
            val descriptionText = "Notifications for boss spawns and guild events"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            val soundUri = if (resId != 0) {
                Uri.parse("android.resource://${context.packageName}/$resId")
            } else {
                Uri.parse("android.resource://${context.packageName}/${R.raw.terran_launch}")
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Note: We don't delete old channels here to avoid notification delivery issues,
            // but the new sound will use the new channel ID.
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBossNotification(bossName: String, minutesRemaining: Int) {
        // Ensure channel exists for current sound preference
        createNotificationChannel()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, bossName.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or UpdateIntentFlags.getUpdateCurrentFlag()
        )

        val soundName = preferenceManager.notificationSound.value
        val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
        val soundUri = if (resId != 0) {
            Uri.parse("android.resource://${context.packageName}/$resId")
        } else {
            Uri.parse("android.resource://${context.packageName}/${R.raw.terran_launch}")
        }

        val title = "$bossName in $minutesRemaining mins!"
        val message = "Prepare for the kill! Boss spawns in $minutesRemaining minutes."

        val builder = NotificationCompat.Builder(context, getChannelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(bossName.hashCode(), builder.build())
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show notification", e)
        }
    }
}

object UpdateIntentFlags {
    fun getUpdateCurrentFlag(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT
    }
}
