package com.elysium.guild.repository

import com.elysium.guild.models.*
import com.elysium.guild.network.ElysiumApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val apiService: ElysiumApiService
) {
    private val gson = Gson()
    
    suspend fun getAttendanceLeaderboard(period: String? = null, subPeriod: String? = null): List<AttendanceLeaderboardEntry> {
        return try {
            val response = apiService.getLeaderboard("attendance", period, subPeriod, 100)
            if (response.isSuccessful && response.body()?.success == true) {
                val dataElement = response.body()?.data
                if (dataElement != null) {
                    val typeToken = object : TypeToken<List<AttendanceLeaderboardEntry>>() {}.type
                    gson.fromJson(dataElement, typeToken)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getPointsLeaderboard(): List<PointsLeaderboardEntry> {
        return try {
            val response = apiService.getLeaderboard("points", null, null, 100)
            if (response.isSuccessful && response.body()?.success == true) {
                val dataElement = response.body()?.data
                if (dataElement != null) {
                    val typeToken = object : TypeToken<List<PointsLeaderboardEntry>>() {}.type
                    gson.fromJson(dataElement, typeToken)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
