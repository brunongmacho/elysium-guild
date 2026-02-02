package com.elysium.guild.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.elysium.guild.repository.BossTimersRepository
import com.elysium.guild.repository.EventsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.TimeUnit

@HiltWorker
class BossNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bossRepository: BossTimersRepository,
    private val eventsRepository: EventsRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BossWorker", "Worker execution started - Scheduling precise alarms")
        
        return try {
            val now = Clock.System.now()

            // 1. Handle Boss Notifications (if enabled)
            if (preferenceManager.bossNotificationsEnabled) {
                val bosses = bossRepository.getBossTimers()
                bosses.forEach { boss ->
                    val spawnTimeStr = boss.nextSpawnTime ?: return@forEach
                    val spawnTime = try { Instant.parse(spawnTimeStr) } catch (e: Exception) { return@forEach }
                    
                    // 10 minutes before boss spawn
                    scheduleExactAlarm(boss.bossName, spawnTime, 10, isEvent = false)
                }
            }

            // 2. Handle Guild Event Notifications (if enabled)
            if (preferenceManager.eventNotificationsEnabled) {
                val events = eventsRepository.getEvents()
                events.forEach { event ->
                    val startTimeStr = event.startTime ?: return@forEach
                    val startTime = try { Instant.parse(startTimeStr) } catch (e: Exception) { return@forEach }
                    
                    // 10 minutes before event starts
                    scheduleExactAlarm(event.name, startTime, 10, isEvent = true)
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }

    private fun scheduleExactAlarm(name: String, time: Instant, minutesBefore: Int, isEvent: Boolean) {
        val alarmTimeMs = time.toEpochMilliseconds() - (minutesBefore * 60 * 1000)
        val nowMs = Clock.System.now().toEpochMilliseconds()

        if (alarmTimeMs <= nowMs) return // Already passed

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, BossAlarmReceiver::class.java).apply {
            putExtra("BOSS_NAME", name) // Receiver uses BOSS_NAME for the title
            putExtra("MINUTES_REMAINING", minutesBefore)
            putExtra("IS_EVENT", isEvent)
        }
        
        // Unique ID: Use name hash + offset to avoid collisions between boss and events
        val requestCode = if (isEvent) name.hashCode() + 1 else name.hashCode()
        
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
                Log.d("BossWorker", "Scheduled 10m alarm for ${if (isEvent) "event" else "boss"}: $name")
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "BossNotificationWorkPeriodic"
        private const val ONE_TIME_WORK_NAME = "BossNotificationWorkImmediate"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val immediateRequest = OneTimeWorkRequestBuilder<BossNotificationWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, immediateRequest)

            val periodicRequest = PeriodicWorkRequestBuilder<BossNotificationWorker>(1, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodicRequest)
        }
    }
}
