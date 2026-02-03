package com.elysium.guild

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.elysium.guild.ui.navigation.ElysiumNavigation
import com.elysium.guild.ui.theme.ElysiumGuildTheme
import com.elysium.guild.utils.BossNotificationWorker
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.viewmodel.ProfileViewModel
import com.elysium.guild.viewmodel.UpdateState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val profileViewModel: ProfileViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            // Schedule immediately once permission is granted
            BossNotificationWorker.schedule(this)
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        super.onCreate(savedInstanceState)
        
        // Request Notification Permission for Android 13+
        askNotificationPermission()
        
        // Schedule Boss Notifications (it will skip if already scheduled)
        BossNotificationWorker.schedule(this)

        // Silent check for updates at startup
        profileViewModel.checkForUpdates(silent = true)
        
        setContent {
            val themeMode by preferenceManager.themeMode.collectAsState()
            val updateState by profileViewModel.updateState.collectAsState()
            
            ElysiumGuildTheme(themeMode = themeMode) {
                val systemUiController = rememberSystemUiController()
                val darkTheme = when (themeMode) {
                    Constants.THEME_LIGHT -> false
                    Constants.THEME_DARK -> true
                    else -> isSystemInDarkTheme()
                }
                
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = !darkTheme
                    )
                }

                Box {
                    ElysiumNavigation(
                        preferenceManager = preferenceManager
                    )

                    // Handle Global Update UI Prompts
                    if (updateState is UpdateState.UpdateAvailable) {
                        val state = updateState as UpdateState.UpdateAvailable
                        AlertDialog(
                            onDismissRequest = { profileViewModel.resetUpdateState() },
                            title = { Text("Update Available") },
                            text = {
                                Column {
                                    Text("A new version (${state.updateInfo.latestVersionName}) is available.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(state.updateInfo.releaseNotes, style = MaterialTheme.typography.bodySmall)
                                    
                                    val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        packageManager.canRequestPackageInstalls()
                                    } else true

                                    if (!canInstall) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Note: You need to allow 'Install from Unknown Sources' in settings for the update to run.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { profileViewModel.downloadAndInstall(state.updateInfo) }) {
                                    Text("Update Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { profileViewModel.resetUpdateState() }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}
