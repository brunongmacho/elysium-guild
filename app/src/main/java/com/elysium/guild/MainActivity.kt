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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.elysium.guild.ui.navigation.ElysiumNavigation
import com.elysium.guild.ui.theme.ElysiumGuildTheme
import com.elysium.guild.utils.BossNotificationWorker
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

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
        
        setContent {
            val themeMode by preferenceManager.themeMode.collectAsState()
            
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
                
                ElysiumNavigation(
                    preferenceManager = preferenceManager
                )
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
