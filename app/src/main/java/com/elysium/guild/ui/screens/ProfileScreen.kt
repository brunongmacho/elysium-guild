package com.elysium.guild.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.elysium.guild.R
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.viewmodel.ProfileViewModel
import com.elysium.guild.viewmodel.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Preference States
    val themeMode by preferenceManager.themeMode.collectAsState()
    val hapticEnabled by preferenceManager.hapticEnabled.collectAsState()
    val savedSound by preferenceManager.notificationSound.collectAsState()
    val savedBossNotif by preferenceManager.bossNotificationsEnabled.collectAsState()
    val savedEventNotif by preferenceManager.eventNotificationsEnabled.collectAsState()

    // Local States for change detection
    var pendingThemeMode by remember(themeMode) { mutableIntStateOf(themeMode) }
    var pendingHapticEnabled by remember(hapticEnabled) { mutableStateOf(hapticEnabled) }
    var pendingSound by remember(savedSound) { mutableStateOf(savedSound) }
    var pendingBossNotif by remember(savedBossNotif) { mutableStateOf(savedBossNotif) }
    var pendingEventNotif by remember(savedEventNotif) { mutableStateOf(savedEventNotif) }

    val hasChanges = pendingThemeMode != themeMode ||
            pendingHapticEnabled != hapticEnabled ||
            pendingSound != savedSound ||
            pendingBossNotif != savedBossNotif ||
            pendingEventNotif != savedEventNotif

    val updateState by viewModel.updateState.collectAsState()

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
                Toast.makeText(context, "App is up to date", Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateState()
            }
        }
        else -> {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 1. APP UPDATES
            SettingsCard(
                title = "App Update",
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
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

            // 2. ALERT & INTERACTION PREFERENCES
            SettingsCard(
                title = "Alert and interaction",
                icon = Icons.Default.Notifications
            ) {
                Column {
                    NotificationToggle(
                        title = "Boss Spawn Alerts",
                        description = "Precise 10m warning for world bosses",
                        checked = pendingBossNotif,
                        onCheckedChange = { pendingBossNotif = it }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    
                    NotificationToggle(
                        title = "Event Reminders",
                        description = "Precise 10m warning for guild activities",
                        checked = pendingEventNotif,
                        onCheckedChange = { pendingEventNotif = it }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    NotificationToggle(
                        title = "Haptic Feedback",
                        description = "Vibrate on status changes and refreshes",
                        checked = pendingHapticEnabled,
                        onCheckedChange = { pendingHapticEnabled = it }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    // SOUND SELECTION
                    SoundSelectionItem(
                        selectedSound = pendingSound,
                        onSoundSelected = { 
                            pendingSound = it
                            playSoundPreview(context, it)
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
                        isSelected = pendingThemeMode == Constants.THEME_LIGHT,
                        onClick = { pendingThemeMode = Constants.THEME_LIGHT },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ThemeOptionButton(
                        text = "Dark",
                        isSelected = pendingThemeMode == Constants.THEME_DARK,
                        onClick = { pendingThemeMode = Constants.THEME_DARK },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ThemeOptionButton(
                        text = "System",
                        isSelected = pendingThemeMode == Constants.THEME_SYSTEM,
                        onClick = { pendingThemeMode = Constants.THEME_SYSTEM },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. PERMISSIONS (Dynamic)
            val needsNotificationPermission = !areNotificationsEnabled
            val needsAlarmPermission = !canScheduleExactAlarms
            val needsBatteryPermission = !isIgnoringBatteryOptimizations
            val needsInstallPermission = !canInstallPackages
            val hasNetworkIssue = !isNetworkAvailable

            val showPermissionsSection = needsNotificationPermission ||
                                       needsAlarmPermission ||
                                       needsBatteryPermission ||
                                       needsInstallPermission ||
                                       hasNetworkIssue

            if (showPermissionsSection) {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsCard(
                    title = "Permissions",
                    icon = Icons.Default.SettingsSuggest
                ) {
                    Column {
                        var first = true

                        if (needsNotificationPermission) {
                            PermissionStatusItem(
                                title = "Push Notifications",
                                statusText = "Denied (Tap to allow)",
                                isActive = false,
                                icon = Icons.Default.NotificationsOff,
                                onClick = { openNotificationSettings(context) }
                            )
                            first = false
                        }

                        if (needsAlarmPermission) {
                            if (!first) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PermissionStatusItem(
                                title = "Exact Alarm Timing",
                                statusText = "Delayed (Tap to allow)",
                                isActive = false,
                                icon = Icons.Default.TimerOff,
                                onClick = { openAlarmSettings(context) }
                            )
                            first = false
                        }

                        if (needsBatteryPermission) {
                            if (!first) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PermissionStatusItem(
                                title = "Battery Optimization",
                                statusText = "Restricted (Tap to fix)",
                                isActive = false,
                                icon = Icons.Default.BatteryAlert,
                                onClick = { requestIgnoreBatteryOptimizations(context) }
                            )
                            first = false
                        }

                        if (hasNetworkIssue) {
                            if (!first) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PermissionStatusItem(
                                title = "Network Access",
                                statusText = "No Connection",
                                isActive = false,
                                icon = Icons.Default.WifiOff,
                                onClick = { /* Check network settings */ }
                            )
                            first = false
                        }

                        if (needsInstallPermission) {
                            if (!first) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PermissionStatusItem(
                                title = "Install APKs",
                                statusText = "Blocked (Tap to allow)",
                                isActive = false,
                                icon = Icons.Default.Error,
                                onClick = { openInstallUnknownAppsSettings(context) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Extra space for the floating bar
        }

        // FLOATING SAVE/CANCEL BAR
        AnimatedVisibility(
            visible = hasChanges,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            // Cancel: Reset local state to saved state
                            pendingThemeMode = themeMode
                            pendingHapticEnabled = hapticEnabled
                            pendingSound = savedSound
                            pendingBossNotif = savedBossNotif
                            pendingEventNotif = savedEventNotif
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel")
                    }

                    VerticalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

                    TextButton(
                        onClick = {
                            // Save: Apply changes to preferenceManager
                            preferenceManager.setThemeMode(pendingThemeMode)
                            preferenceManager.setHapticFeedbackEnabled(pendingHapticEnabled)
                            preferenceManager.setNotificationSound(pendingSound)
                            preferenceManager.setBossNotificationsEnabled(pendingBossNotif)
                            preferenceManager.setEventNotificationsEnabled(pendingEventNotif)
                            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSelectionItem(
    selectedSound: String,
    onSoundSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Sort and reorder logic
    val sounds = remember(selectedSound) {
        listOf(
            "terran_launch" to "Terran Launch",
            "siege_tank" to "Siege Tank",
            "marine_want" to "Marine Want",
            "terran_addon" to "Terran Addon",
            "terran_attack" to "Terran Attack",
            "goliath_target" to "Goliath Target",
            "science_vessel" to "Science Vessel",
            "ghost_reporting" to "Ghost Reporting",
            "terran_detected" to "Terran Detected",
            "ghost_exterminator" to "Ghost Exterminator"
        )
        .sortedBy { it.second } // 1. Alphabetical sort
        .let { sortedList ->
            // 2. Move selected to top
            val (selected, rest) = sortedList.partition { it.first == selectedSound }
            selected + rest
        }
    }

    val currentDisplayName = sounds.find { it.first == selectedSound }?.second ?: selectedSound

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Notification Sound",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = currentDisplayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to change",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .exposedDropdownSize()
                    .padding(vertical = 4.dp)
            ) {
                sounds.forEach { (id, name) ->
                    val isSelected = id == selectedSound
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSoundSelected(id)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private var mediaPlayer: MediaPlayer? = null

private fun playSoundPreview(context: Context, soundName: String) {
    try {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        
        val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
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
