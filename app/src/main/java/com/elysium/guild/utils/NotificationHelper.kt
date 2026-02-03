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
        // Using a base ID to create unique channels per sound
        private const val BASE_CHANNEL_ID = "boss_event_alerts"
    }

    // Re-create channel whenever a new helper is injected or sound preference changes.
    init {
        createNotificationChannel()
    }

    private fun getChannelIdForCurrentSound(): String {
        val soundName = preferenceManager.notificationSound.value
        return "${BASE_CHANNEL_ID}_$soundName"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundName = preferenceManager.notificationSound.value
            val channelId = getChannelIdForCurrentSound()

            val name = "Boss & Event Alerts"
            val descriptionText = "Notifications for boss spawns and guild events."
            val importance = NotificationManager.IMPORTANCE_HIGH

            // Correctly get the resource ID for the selected sound
            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            
            // Correctly form the sound URI, with a reliable fallback to the default sound's ID.
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
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Notification channel '$channelId' created with sound '$soundUri'")
        }
    }

    fun showBossNotification(bossName: String, minutesRemaining: Int) {
        // This function is called when the sound preference changes, ensuring the channel is up-to-date.
        createNotificationChannel()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            bossName.hashCode(), 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "$bossName in $minutesRemaining mins!"
        val message = "Prepare for the kill! The boss spawns in $minutesRemaining minutes."

        // The sound is now set on the CHANNEL, not on the builder.
        // This is the correct modern approach.
        val builder = NotificationCompat.Builder(context, getChannelIdForCurrentSound())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // DO NOT set sound here; it's handled by the channel.
            // .setSound(soundUri) 

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // The ID should be unique per notification to avoid them overwriting each other.
            notificationManager.notify(bossName.hashCode(), builder.build())
            Log.d("NotificationHelper", "Notification sent for $bossName on channel ${getChannelIdForCurrentSound()}")
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Failed to show notification due to permission issue.", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show notification", e)
        }
    }
}
