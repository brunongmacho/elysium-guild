package com.elysium.guild.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApiService {
    @GET
    suspend fun getUpdateManifest(@Url url: String = "https://raw.githubusercontent.com/brunongmacho/elysium-guild/main/update-manifest.json"): Response<UpdateManifest>
}

data class UpdateManifest(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean
)
