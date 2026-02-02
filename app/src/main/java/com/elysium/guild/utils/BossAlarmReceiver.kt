package com.elysium.guild.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BossAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bossName = intent.getStringExtra("BOSS_NAME") ?: "Unknown Boss"
        val minutesRemaining = intent.getIntExtra("MINUTES_REMAINING", 0)
        
        Log.d("BossAlarmReceiver", "Alarm triggered for $bossName ($minutesRemaining mins before spawn)")
        
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showBossNotification(bossName, minutesRemaining)
    }
}
