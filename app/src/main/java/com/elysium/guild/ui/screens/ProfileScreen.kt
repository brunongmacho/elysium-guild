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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.elysium.guild.R
import com.elysium.guild.ui.components.*
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.viewmodel.ProfileViewModel
import com.elysium.guild.viewmodel.UpdateState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    
    // Parallax scroll offset tracking
    val scrollOffset = remember { derivedStateOf { scrollState.value.toFloat() } }
    
    // Preference States
    val themeMode by preferenceManager.themeMode.collectAsState()
    val hapticEnabled by preferenceManager.hapticEnabled.collectAsState()
    val vibrateOnly by preferenceManager.vibrateOnly.collectAsState()
    val savedSound by preferenceManager.notificationSound.collectAsState()
    val savedBossNotif by preferenceManager.bossNotificationsEnabled.collectAsState()
    val savedEventNotif by preferenceManager.eventNotificationsEnabled.collectAsState()
    val savedBubbleEnabled by preferenceManager.floatingBubbleEnabled.collectAsState()
    val savedLocalTimezone by preferenceManager.useLocalTimezone.collectAsState()

    // Local States for change detection
    var pendingThemeMode by remember(themeMode) { mutableIntStateOf(themeMode) }
    var pendingHapticEnabled by remember(hapticEnabled) { mutableStateOf(hapticEnabled) }
    var pendingVibrateOnly by remember(vibrateOnly) { mutableStateOf(vibrateOnly) }
    var pendingSound by remember(savedSound) { mutableStateOf(savedSound) }
    var pendingBossNotif by remember(savedBossNotif) { mutableStateOf(savedBossNotif) }
    var pendingEventNotif by remember(savedEventNotif) { mutableStateOf(savedEventNotif) }
    var pendingBubbleEnabled by remember(savedBubbleEnabled) { mutableStateOf(savedBubbleEnabled) }
    var pendingLocalTimezone by remember(savedLocalTimezone) { mutableStateOf(savedLocalTimezone) }

    var showSaveSuccess by remember { mutableStateOf(false) }

    val hasChanges = pendingThemeMode != themeMode ||
            pendingHapticEnabled != hapticEnabled ||
            pendingVibrateOnly != vibrateOnly ||
            pendingSound != savedSound ||
            pendingBossNotif != savedBossNotif ||
            pendingEventNotif != savedEventNotif ||
            pendingBubbleEnabled != savedBubbleEnabled ||
            pendingLocalTimezone != savedLocalTimezone

    val updateState by viewModel.updateState.collectAsState()

    // Permission States
    var areNotificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var canScheduleExactAlarms by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    val allPermissionsEnabled = areNotificationsEnabled && canScheduleExactAlarms && canDrawOverlays && isIgnoringBatteryOptimizations

    // Donation Sheet State
    var showDonationSheet by remember { mutableStateOf(false) }
    val donationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                canScheduleExactAlarms = canScheduleExactAlarms(context)
                canDrawOverlays = Settings.canDrawOverlays(context)
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showSaveSuccess) {
        if (showSaveSuccess) {
            delay(3000)
            showSaveSuccess = false
        }
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DynamicElysiumBackground(scrollOffset = scrollOffset.value) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Centered Header (Picture removed as requested)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Constants.TITLE_SETTINGS,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = Constants.SUBTITLE_SETTINGS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. SYSTEM HEALTH (Hidden if everything is enabled)
                    AnimatedVisibility(
                        visible = !allPermissionsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SettingsCard(
                            title = "System Health",
                            icon = Icons.Default.HealthAndSafety
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!areNotificationsEnabled) {
                                    PermissionStatusItem(
                                        title = "Push Notifications",
                                        statusText = "Denied (Tap to fix)",
                                        isActive = false,
                                        icon = Icons.Default.NotificationsOff,
                                        onClick = { openNotificationSettings(context) }
                                    )
                                }
                                if (!canScheduleExactAlarms) {
                                    PermissionStatusItem(
                                        title = "Exact Alarms",
                                        statusText = "Delayed (Tap to fix)",
                                        isActive = false,
                                        icon = Icons.Default.TimerOff,
                                        onClick = { openAlarmSettings(context) }
                                    )
                                }
                                if (!canDrawOverlays) {
                                    PermissionStatusItem(
                                        title = "Overlay Bubble",
                                        statusText = "Required (Tap to fix)",
                                        isActive = false,
                                        icon = Icons.Default.LayersClear,
                                        onClick = { openOverlaySettings(context) }
                                    )
                                }
                                if (!isIgnoringBatteryOptimizations) {
                                    PermissionStatusItem(
                                        title = "Battery Optimization",
                                        statusText = "Restricted (Tap to fix)",
                                        isActive = false,
                                        icon = Icons.Default.BatteryAlert,
                                        onClick = { requestIgnoreBatteryOptimizations(context) }
                                    )
                                }
                            }
                        }
                    }

                    if (!allPermissionsEnabled) Spacer(modifier = Modifier.height(16.dp))

                    // 2. APP UPDATES
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
                                
                                Surface(
                                    onClick = { viewModel.checkForUpdates() },
                                    enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.2f else 0.1f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (updateState is UpdateState.Checking) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                        } else {
                                            Text(
                                                text = "Check",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
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

                    // 3. ALERT & INTERACTION PREFERENCES
                    SettingsCard(
                        title = "Alert and interaction",
                        icon = Icons.Default.Notifications
                    ) {
                        Column {
                            NotificationToggle(
                                title = "Boss Spawn Alerts",
                                description = "Precise 10m warning for world bosses",
                                checked = pendingBossNotif,
                                onCheckedChange = { pendingBossNotif = it },
                                icon = Icons.Default.Timer,
                                iconColor = Color(0xFF10B981)
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            
                            NotificationToggle(
                                title = "Event Reminders",
                                description = "Precise 10m warning for guild activities",
                                checked = pendingEventNotif,
                                onCheckedChange = { pendingEventNotif = it },
                                icon = Icons.Default.Event,
                                iconColor = Color(0xFF6366F1)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                            NotificationToggle(
                                title = "Floating Boss Timer",
                                description = "Show a floating bubble over other apps",
                                checked = pendingBubbleEnabled,
                                onCheckedChange = { 
                                    if (it && !canDrawOverlays) {
                                        pendingBubbleEnabled = false
                                        openOverlaySettings(context)
                                    } else {
                                        pendingBubbleEnabled = it
                                    }
                                },
                                icon = Icons.Default.AdsClick,
                                iconColor = Color(0xFFF59E0B)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                            NotificationToggle(
                                title = "Vibrate Only",
                                description = "Silent alerts with haptic feedback",
                                checked = pendingVibrateOnly,
                                onCheckedChange = { pendingVibrateOnly = it },
                                icon = Icons.Default.NotificationsPaused,
                                iconColor = Color(0xFF94A3B8)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                            NotificationToggle(
                                title = "Haptic Feedback",
                                description = "Vibrate on status changes and refreshes",
                                checked = pendingHapticEnabled,
                                onCheckedChange = { pendingHapticEnabled = it },
                                icon = Icons.Default.Vibration,
                                iconColor = Color(0xFFEC4899)
                            )

                            if (!pendingVibrateOnly) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                                SoundSelectionItem(
                                    selectedSound = pendingSound,
                                    onSoundSelected = { 
                                        pendingSound = it
                                        playSoundPreview(context, it)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. APPEARANCE & TIMEZONE
                    SettingsCard(
                        title = "Appearance and Time",
                        icon = Icons.Default.Palette
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeOptionButton(
                                    text = "Light",
                                    isSelected = pendingThemeMode == Constants.THEME_LIGHT,
                                    onClick = { pendingThemeMode = Constants.THEME_LIGHT },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionButton(
                                    text = "Dark",
                                    isSelected = pendingThemeMode == Constants.THEME_DARK,
                                    onClick = { pendingThemeMode = Constants.THEME_DARK },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionButton(
                                    text = "System",
                                    isSelected = pendingThemeMode == Constants.THEME_SYSTEM,
                                    onClick = { pendingThemeMode = Constants.THEME_SYSTEM },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                            NotificationToggle(
                                title = "Use Local Timezone",
                                description = "Show times in your device's timezone",
                                checked = pendingLocalTimezone,
                                onCheckedChange = { pendingLocalTimezone = it },
                                icon = Icons.Default.Language,
                                iconColor = Color(0xFF06B6D4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. GUILD SUPPORT
                    SettingsCard(
                        title = "Guild Support",
                        icon = Icons.Default.Favorite
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDonationSheet = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "Donate to Guild", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Help us keep the servers running", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(120.dp))
                }

                // GLASSY SAVE BAR
                AnimatedVisibility(
                    visible = hasChanges,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    ElysiumGlassCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(72.dp),
                        cornerRadius = 36.dp,
                        glowColor = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    pendingThemeMode = themeMode
                                    pendingHapticEnabled = hapticEnabled
                                    pendingVibrateOnly = vibrateOnly
                                    pendingSound = savedSound
                                    pendingBossNotif = savedBossNotif
                                    pendingEventNotif = savedEventNotif
                                    pendingBubbleEnabled = savedBubbleEnabled
                                    pendingLocalTimezone = savedLocalTimezone
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Discard")
                            }

                            VerticalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            TextButton(
                                onClick = {
                                    preferenceManager.setThemeMode(pendingThemeMode)
                                    preferenceManager.setHapticFeedbackEnabled(pendingHapticEnabled)
                                    preferenceManager.setVibrateOnlyEnabled(pendingVibrateOnly)
                                    preferenceManager.setNotificationSound(pendingSound)
                                    preferenceManager.setBossNotificationsEnabled(pendingBossNotif)
                                    preferenceManager.setEventNotificationsEnabled(pendingEventNotif)
                                    preferenceManager.setFloatingBubbleEnabled(pendingBubbleEnabled)
                                    preferenceManager.setUseLocalTimezone(pendingLocalTimezone)
                                    showSaveSuccess = true
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = CircleShape,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Settings", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Save Success Notification
                AnimatedVisibility(
                    visible = showSaveSuccess,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
                ) {
                    ElysiumGlassCard(
                        modifier = Modifier.padding(horizontal = 32.dp).widthIn(max = 300.dp),
                        statusColor = Constants.COLOR_SUCCESS,
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Constants.COLOR_SUCCESS, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(text = "Configuration Saved", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // DONATION BOTTOM SHEET (QR)
                if (showDonationSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showDonationSheet = false },
                        sheetState = donationSheetState,
                        containerColor = Color.Transparent,
                        dragHandle = null
                    ) {
                        ElysiumGlassCard(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
                            cornerRadius = 28.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(text = Constants.DONATION_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = Constants.DONATION_DESC, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(24.dp))
                                Surface(
                                    modifier = Modifier.size(240.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.White
                                ) {
                                    val qrResId = context.resources.getIdentifier(Constants.RES_QR_DONATION, "drawable", context.packageName)
                                    if (qrResId != 0) {
                                        Image(painter = painterResource(id = qrResId), contentDescription = "QR Code", modifier = Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { showDonationSheet = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Text("Done", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
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
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else true
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager.activeNetwork ?: return false
    } else return true
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(settingsIntent)
        }
    }
}

private fun openAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
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
