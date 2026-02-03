package com.elysium.guild.repository

import android.util.Log
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
        try {
            val response = apiService.getBossTimers()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success == true) {
                    val bosses = body.bosses
                    _bossDataChanged.tryEmit(Unit)
                    return bosses ?: emptyList()
                }
            }
            // Throw exception to be caught by ViewModel and displayed in UI
            val errorMsg = "API Error: ${response.code()} ${response.message()}"
            Log.e("BossTimersRepository", errorMsg)
            throw Exception(errorMsg)
        } catch (e: Exception) {
            Log.e("BossTimersRepository", "Failed to fetch boss timers", e)
            throw e
        }
    }
}
