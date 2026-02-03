package com.elysium.guild.repository

import android.util.Log
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
        try {
            val response = apiService.getLeaderboard("attendance", period, subPeriod, 100)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success == true) {
                    val dataElement = body.data
                    if (dataElement != null) {
                        val typeToken = object : TypeToken<List<AttendanceLeaderboardEntry>>() {}.type
                        return gson.fromJson(dataElement, typeToken)
                    }
                }
            }
            val errorMsg = "Attendance API Error: ${response.code()} ${response.message()}"
            Log.e("LeaderboardRepository", errorMsg)
            throw Exception(errorMsg)
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Attendance load failed", e)
            throw e
        }
    }
    
    suspend fun getPointsLeaderboard(): List<PointsLeaderboardEntry> {
        try {
            val response = apiService.getLeaderboard("points", null, null, 100)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success == true) {
                    val dataElement = body.data
                    if (dataElement != null) {
                        val typeToken = object : TypeToken<List<PointsLeaderboardEntry>>() {}.type
                        return gson.fromJson(dataElement, typeToken)
                    }
                }
            }
            val errorMsg = "Points API Error: ${response.code()} ${response.message()}"
            Log.e("LeaderboardRepository", errorMsg)
            throw Exception(errorMsg)
        } catch (e: Exception) {
            Log.e("LeaderboardRepository", "Points load failed", e)
            throw e
        }
    }
}
