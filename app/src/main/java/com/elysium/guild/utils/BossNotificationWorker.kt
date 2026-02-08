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
            if (preferenceManager.bossNotificationsEnabled.value) {
                val bosses = bossRepository.getBossTimers()
                bosses.forEach { boss ->
                    val spawnTimeStr = boss.nextSpawnTime ?: return@forEach
                    val spawnTime = try { Instant.parse(spawnTimeStr) } catch (e: Exception) { return@forEach }
                    
                    scheduleAlarmSafely(boss.bossName, spawnTime, preferenceManager.bossNotificationOffset, isEvent = false)
                }
            }

            // 2. Handle Guild Event Notifications (if enabled)
            if (preferenceManager.eventNotificationsEnabled.value) {
                val events = eventsRepository.getEvents()
                events.forEach { event ->
                    val startTimeStr = event.startTime ?: return@forEach
                    val startTime = try { Instant.parse(startTimeStr) } catch (e: Exception) { return@forEach }
                    val endTime = event.endTime?.let { try { Instant.parse(it) } catch(e: Exception) { null } }
                    
                    // If event is currently running (now is between start and end), 
                    // we don't schedule a start notification, but we maintain the 'READY' status in UI.
                    val isRunning = endTime?.let { now >= startTime && now < it } ?: false
                    
                    if (!isRunning) {
                        scheduleAlarmSafely(event.name, startTime, Constants.DEFAULT_NOTIFICATION_OFFSET_MINUTES, isEvent = true)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("BossWorker", "Error in worker: ${e.message}")
            Result.retry()
        }
    }

    private fun scheduleAlarmSafely(name: String, time: Instant, minutesBefore: Int, isEvent: Boolean) {
        val alarmTimeMs = time.toEpochMilliseconds() - (minutesBefore * 60 * 1000)
        val nowMs = Clock.System.now().toEpochMilliseconds()

        if (alarmTimeMs <= nowMs) return // Already passed

        val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)
        val intent = Intent(applicationContext, BossAlarmReceiver::class.java).apply {
            putExtra(Constants.EXTRA_BOSS_NAME, name)
            putExtra(Constants.EXTRA_MINUTES_REMAINING, minutesBefore)
            putExtra(Constants.EXTRA_IS_EVENT, isEvent)
        }
        
        val requestCode = if (isEvent) name.hashCode() + 1 else name.hashCode()
        
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
            }
        } catch (se: SecurityException) {
            Log.e("BossWorker", "SecurityException during alarm scheduling", se)
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BossNotificationWorker"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val immediateRequest = OneTimeWorkRequestBuilder<BossNotificationWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            workManager.enqueueUniqueWork(
                Constants.WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )

            val periodicRequest = PeriodicWorkRequestBuilder<BossNotificationWorker>(1, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }
    }
}
