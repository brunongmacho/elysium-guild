package com.elysium.guild.repository

import android.util.Log
import com.elysium.guild.database.BossTimerDao
import com.elysium.guild.models.*
import com.elysium.guild.network.ElysiumApiService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BossTimersRepository @Inject constructor(
    private val apiService: ElysiumApiService,
    private val bossTimerDao: BossTimerDao
) {
    private val _bossDataChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val bossDataChanged: SharedFlow<Unit> = _bossDataChanged.asSharedFlow()

    // Cache of boss statuses to prevent unnecessary UI refreshes
    private val previousStatuses = mutableMapOf<String, String>()

    suspend fun getBossTimers(): List<BossTimer> {
        return try {
            val response = apiService.getBossTimers()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success == true) {
                    val bosses = body.bosses ?: emptyList()

                    // Only notify observers if there's a significant status change (tagging change)
                    // This prevents the leaderboard and other observers from refreshing
                    // during regular polling when everything is still in the "tracking" state.
                    val hasStatusChanged = detectStatusChanges(bosses)

                    // Save to local database for offline support and better stability
                    saveBossesToLocal(bosses)

                    if (hasStatusChanged) {
                        _bossDataChanged.tryEmit(Unit)
                    }
                    return bosses
                }
            }

            // If API fails, fall back to local database
            Log.w("BossTimersRepository", "API failed, falling back to local database")
            getCachedBosses()
        } catch (e: Exception) {
            Log.e("BossTimersRepository", "Failed to fetch boss timers, using cache", e)
            getCachedBosses()
        }
    }

    /**
     * Detects if any boss has transitioned between statuses (tracking, soon, ready, overdue).
     * Returns true if at least one boss has changed its "tagging".
     */
    private fun detectStatusChanges(newBosses: List<BossTimer>): Boolean {
        var anyChange = false
        newBosses.forEach { boss ->
            val oldStatus = previousStatuses[boss.bossName]
            if (oldStatus != boss.status) {
                anyChange = true
            }
            previousStatuses[boss.bossName] = boss.status
        }
        return anyChange
    }

    private suspend fun getCachedBosses(): List<BossTimer> {
        return try {
            val entities = bossTimerDao.getAllBossTimers().first()
            entities.map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.e("BossTimersRepository", "Failed to fetch from cache", e)
            emptyList()
        }
    }

    private suspend fun saveBossesToLocal(bosses: List<BossTimer>) {
        try {
            val entities = bosses.map { it.toEntity() }
            bossTimerDao.clearAll()
            bossTimerDao.insertAll(entities)
        } catch (e: Exception) {
            Log.e("BossTimersRepository", "Failed to save to cache", e)
        }
    }
}

// Extension functions for mapping between Domain and Entity models
fun BossTimer.toEntity(): BossTimerEntity {
    return BossTimerEntity(
        id = this.bossName,
        name = this.bossName,
        points = this.bossPoints,
        type = this.type,
        nextSpawnTime = this.nextSpawnTime,
        status = this.status,
        imageUrl = this.imageUrl,
        isRotating = this.rotation?.isRotating ?: false,
        currentGuild = this.rotation?.currentGuild
    )
}

fun BossTimerEntity.toDomainModel(): BossTimer {
    return BossTimer(
        bossName = this.name,
        bossPoints = this.points,
        type = this.type,
        imageUrl = this.imageUrl,
        nextSpawnTime = this.nextSpawnTime,
        status = this.status,
        timeRemaining = null, // Will be recalculated by ViewModel
        rotation = RotationInfo(
            isRotating = this.isRotating,
            currentGuild = this.currentGuild
        )
    )
}
