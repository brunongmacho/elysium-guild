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
        private const val BASE_CHANNEL_ID = "boss_event_alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun getChannelIdForCurrentSound(): String {
        val soundName = preferenceManager.notificationSound.value
        val vibrateOnly = preferenceManager.vibrateOnly.value
        val hapticEnabled = preferenceManager.hapticEnabled.value
        
        return if (vibrateOnly) {
            "${BASE_CHANNEL_ID}_vibrate_mode"
        } else {
            "${BASE_CHANNEL_ID}_${soundName}_haptic_${hapticEnabled}"
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundName = preferenceManager.notificationSound.value
            val vibrateOnly = preferenceManager.vibrateOnly.value
            val hapticEnabled = preferenceManager.hapticEnabled.value
            val channelId = getChannelIdForCurrentSound()

            val name = "Boss & Event Alerts"
            val descriptionText = "Notifications for boss spawns and guild events."
            val importance = NotificationManager.IMPORTANCE_HIGH

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                // If Vibrate Only is ON, we ALWAYS vibrate.
                // If Vibrate Only is OFF, we follow the Haptic toggle.
                enableVibration(vibrateOnly || hapticEnabled)
                setShowBadge(true)
                
                if (vibrateOnly) {
                    setSound(null, null)
                } else {
                    val resId = try {
                        val id = context.resources.getIdentifier(soundName, "raw", context.packageName)
                        if (id != 0) id else R.raw.terran_launch
                    } catch (e: Exception) {
                        R.raw.terran_launch
                    }
                    
                    val soundUri = Uri.parse("android.resource://${context.packageName}/$resId")
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()

                    try {
                        setSound(soundUri, audioAttributes)
                    } catch (e: Exception) {
                        Log.e("NotificationHelper", "Failed to set sound on channel: ${e.message}")
                    }
                }
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBossNotification(bossName: String, minutesRemaining: Int) {
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

        val builder = NotificationCompat.Builder(context, getChannelIdForCurrentSound())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(bossName.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException: Notification permission may have been revoked.", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show notification: ${e.message}")
        }
    }
}
