package com.elysium.guild.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.viewmodel.ProfileViewModel
import com.elysium.guild.viewmodel.UpdateState

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeMode by preferenceManager.themeMode.collectAsState()
    val hapticEnabled by preferenceManager.hapticEnabled.collectAsState()

    val updateState by viewModel.updateState.collectAsState()

    var bossNotifications by remember { mutableStateOf(preferenceManager.bossNotificationsEnabled) }
    var eventNotifications by remember { mutableStateOf(preferenceManager.eventNotificationsEnabled) }
    
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var areNotificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var canScheduleExactAlarms by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var isNetworkAvailable by remember { mutableStateOf(isNetworkAvailable(context)) }
    var canInstallPackages by remember { mutableStateOf(canInstallPackages(context)) }

    val currentVersionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
                areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                canScheduleExactAlarms = canScheduleExactAlarms(context)
                isNetworkAvailable = isNetworkAvailable(context)
                canInstallPackages = canInstallPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle Update UI Prompts
    when (val state = updateState) {
        is UpdateState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUpdateState() },
                title = { Text("Update Available") },
                text = {
                    Column {
                        Text("A new version (${state.updateInfo.latestVersionName}) is available.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.updateInfo.releaseNotes, style = MaterialTheme.typography.bodySmall)
                        
                        if (!canInstallPackages) {
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
                    Button(onClick = { viewModel.downloadAndInstall(state.updateInfo) }) {
                        Text("Update Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetUpdateState() }) {
                        Text("Later")
                    }
                }
            )
        }
        UpdateState.UpToDate -> {
            LaunchedEffect(Unit) {
                viewModel.resetUpdateState()
            }
        }
        else -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 0. APP UPDATES
        SettingsCard(
            title = "App Updates",
            icon = Icons.Default.SystemUpdate
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Software Update", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when (updateState) {
                                UpdateState.Checking -> "Checking for updates..."
                                UpdateState.Downloading -> "Downloading update..."
                                else -> "Keep your app up to date"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Check")
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Installation Permission Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Install Permissions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (canInstallPackages) "Authorized to install APKs" else "Installation blocked (Tap to allow)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canInstallPackages) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { openInstallUnknownAppsSettings(context) }) {
                        Icon(
                            imageVector = if (canInstallPackages) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (canInstallPackages) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Current Version: $currentVersionName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. SYSTEM PERMISSIONS & STATUS
        SettingsCard(
            title = "System Permissions",
            icon = Icons.Default.SettingsSuggest
        ) {
            Column {
                PermissionStatusItem(
                    title = "Push Notifications",
                    statusText = if (areNotificationsEnabled) "Authorized" else "Denied (Tap to allow)",
                    isActive = areNotificationsEnabled,
                    icon = if (areNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    onClick = { openNotificationSettings(context) }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                
                PermissionStatusItem(
                    title = "Exact Alarm Timing",
                    statusText = if (canScheduleExactAlarms) "Precise" else "Delayed (Tap to allow)",
                    isActive = canScheduleExactAlarms,
                    icon = if (canScheduleExactAlarms) Icons.Default.Timer else Icons.Default.TimerOff,
                    onClick = { openAlarmSettings(context) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                PermissionStatusItem(
                    title = "Battery Optimization",
                    statusText = if (isIgnoringBatteryOptimizations) "Unrestricted" else "Restricted (Tap to fix)",
                    isActive = isIgnoringBatteryOptimizations,
                    icon = if (isIgnoringBatteryOptimizations) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                    onClick = { requestIgnoreBatteryOptimizations(context) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                PermissionStatusItem(
                    title = "Network Access",
                    statusText = if (isNetworkAvailable) "Connected" else "No Connection",
                    isActive = isNetworkAvailable,
                    icon = if (isNetworkAvailable) Icons.Default.Wifi else Icons.Default.WifiOff,
                    onClick = { /* Check network settings */ }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // 2. ALERT & INTERACTION PREFERENCES
        SettingsCard(
            title = "Alert & Interaction",
            icon = Icons.Default.Notifications
        ) {
            Column {
                NotificationToggle(
                    title = "Boss Spawn Alerts",
                    description = "Precise 10m warning for world bosses",
                    checked = bossNotifications,
                    onCheckedChange = {
                        bossNotifications = it
                        preferenceManager.bossNotificationsEnabled = it
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                
                NotificationToggle(
                    title = "Event Reminders",
                    description = "Precise 10m warning for guild activities",
                    checked = eventNotifications,
                    onCheckedChange = {
                        eventNotifications = it
                        preferenceManager.eventNotificationsEnabled = it
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                NotificationToggle(
                    title = "Haptic Feedback",
                    description = "Vibrate on status changes and refreshes",
                    checked = hapticEnabled,
                    onCheckedChange = {
                        preferenceManager.hapticFeedbackEnabled = it
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. APPEARANCE
        SettingsCard(
            title = "Appearance",
            icon = Icons.Default.Palette
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThemeOptionButton(
                    text = "Light",
                    isSelected = themeMode == Constants.THEME_LIGHT,
                    onClick = { preferenceManager.setThemeMode(Constants.THEME_LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                ThemeOptionButton(
                    text = "Dark",
                    isSelected = themeMode == Constants.THEME_DARK,
                    onClick = { preferenceManager.setThemeMode(Constants.THEME_DARK) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                ThemeOptionButton(
                    text = "System",
                    isSelected = themeMode == Constants.THEME_SYSTEM,
                    onClick = { preferenceManager.setThemeMode(Constants.THEME_SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionStatusItem(
    title: String,
    statusText: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun canInstallPackages(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.packageManager.canRequestPackageInstalls()
    } else true
}

private fun openInstallUnknownAppsSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else true
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    context.startActivity(intent)
}

private fun openAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent().apply {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            else -> {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
        }
    }
    context.startActivity(intent)
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ThemeOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}
