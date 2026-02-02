package com.elysium.guild.repository

import com.elysium.guild.models.*
import com.elysium.guild.network.ElysiumApiService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BossTimersRepository @Inject constructor(
    private val apiService: ElysiumApiService
) {
    private val _bossDataChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val bossDataChanged: SharedFlow<Unit> = _bossDataChanged.asSharedFlow()

    suspend fun getBossTimers(): List<BossTimer> {
        return try {
            val response = apiService.getBossTimers()
            if (response.isSuccessful && response.body()?.success == true) {
                val bosses = response.body()?.bosses ?: emptyList()
                // Notify that we have fresh data, which might imply leaderboard changes
                _bossDataChanged.tryEmit(Unit)
                bosses
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
