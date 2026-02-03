package com.elysium.guild.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BossAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val bossName = intent.getStringExtra("BOSS_NAME") ?: "Unknown Boss"
        val minutesRemaining = intent.getIntExtra("MINUTES_REMAINING", 0)
        
        Log.d("BossAlarmReceiver", "Alarm triggered for $bossName ($minutesRemaining mins before spawn)")
        
        notificationHelper.showBossNotification(bossName, minutesRemaining)
    }
}
