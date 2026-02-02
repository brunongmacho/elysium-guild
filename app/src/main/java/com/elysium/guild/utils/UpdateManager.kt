package com.elysium.guild.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.elysium.guild.BuildConfig
import com.elysium.guild.network.UpdateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    private val context: Context,
    private val updateApiService: UpdateApiService
) {
    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val apkUrl: String,
        val releaseNotes: String,
        val isForceUpdate: Boolean
    )

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Replace 'USER/REPO/main' with your actual GitHub username, repo name, and branch
            val response = updateApiService.getUpdateManifest()
            if (response.isSuccessful) {
                val manifest = response.body()
                if (manifest != null && manifest.latestVersionCode > BuildConfig.VERSION_CODE) {
                    return@withContext UpdateInfo(
                        latestVersionCode = manifest.latestVersionCode,
                        latestVersionName = manifest.latestVersionName,
                        apkUrl = manifest.apkUrl,
                        releaseNotes = manifest.releaseNotes,
                        isForceUpdate = manifest.isForceUpdate
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun downloadAndInstall(apkUrl: String, fileName: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(apkUrl)
        
        val request = DownloadManager.Request(uri)
            .setTitle("Elysium Guild Update")
            .setDescription("Downloading new version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        // Register receiver for when download is complete
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId == id) {
                    installApk(fileName)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType(contentUri, "application/vnd.android.package-archive")
            }
            context.startActivity(installIntent)
        }
    }
}
