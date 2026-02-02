package com.elysium.guild.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.elysium.guild.R
import com.elysium.guild.di.WidgetEntryPoint
import com.elysium.guild.models.BossTimer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class BossWidget : GlanceAppWidget() {

    companion object {
        private var tickerJob: Job? = null
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        private var cachedBosses: List<BossTimer>? = null
        private var lastFetchTime: Long = 0
        private const val CACHE_DURATION_MS = 10000 // Refresh boss data from DB every 10s

        fun startTicker(context: Context) {
            if (tickerJob?.isActive == true) return
            val appContext = context.applicationContext
            tickerJob = scope.launch {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, BossWidgetReceiver::class.java)

                Log.d("BossWidget", "Starting ticker loop")
                while (isActive) {
                    val ids = appWidgetManager.getAppWidgetIds(componentName)
                    if (ids.isEmpty()) {
                        Log.d("BossWidget", "No active widgets, stopping ticker")
                        break
                    }

                    try {
                        // This triggers provideGlance for all active widgets
                        BossWidget().updateAll(appContext)
                    } catch (e: CancellationException) {
                        // Expected when updateAll cancels previous sessions
                        Log.d("BossWidget", "Ticker updateAll cancelled (expected)")
                    } catch (e: Exception) {
                        Log.e("BossWidget", "Ticker updateAll failed", e)
                    }
                    // Delay between updates to avoid flooding WorkManager
                    delay(10000)
                }
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        
        // Ensure ticker is running whenever a widget is updated or added
        startTicker(appContext)

        val nowMs = Clock.System.now().toEpochMilliseconds()
        
        // Use cached data if fresh enough to avoid DB pressure during UI updates
        val bosses = if (cachedBosses == null || nowMs - lastFetchTime > CACHE_DURATION_MS) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
                val repository = entryPoint.bossTimersRepository()
                repository.getBossTimers().also {
                    cachedBosses = it
                    lastFetchTime = nowMs
                }
            } catch (e: Exception) {
                Log.e("BossWidget", "Failed to fetch boss data", e)
                cachedBosses ?: emptyList()
            }
        } else {
            cachedBosses!!
        }
        
        val nextBosses = findNextBosses(bosses, 8)

        provideContent {
            BossWidgetContent(nextBosses)
        }
    }

    private fun findNextBosses(bosses: List<BossTimer>, limit: Int): List<BossTimer> {
        if (bosses.isEmpty()) return emptyList()
        val now = Clock.System.now()
        return bosses
            .filter { it.nextSpawnTime != null }
            .mapNotNull { boss ->
                try {
                    val spawnTime = Instant.parse(boss.nextSpawnTime!!)
                    boss to spawnTime
                } catch (e: Exception) { null }
            }
            .filter { it.second.toEpochMilliseconds() - now.toEpochMilliseconds() > -600000 }
            .sortedBy { it.second.toEpochMilliseconds() }
            .take(limit)
            .map { it.first }
    }

    @Composable
    private fun BossWidgetContent(bosses: List<BossTimer>) {
        val context = LocalContext.current
        val now = Clock.System.now() // Re-calculated every time provideContent runs
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(android.R.color.black))
                .cornerRadius(16.dp)
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.Top,
                modifier = GlanceModifier.fillMaxSize()
            ) {
                Text(
                    text = "ELYSIUM BOSSES",
                    style = TextStyle(
                        color = ColorProvider(android.R.color.white),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(bottom = 6.dp)
                )
                
                if (bosses.isNotEmpty()) {
                    val rows = bosses.chunked(2)
                    rows.forEach { rowBosses ->
                        Row(
                            modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            rowBosses.forEach { boss ->
                                Box(
                                    modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BossItem(context, boss, now)
                                }
                            }
                            if (rowBosses.size < 2) {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                        }
                    }
                } else {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No boss data",
                            style = TextStyle(color = ColorProvider(android.R.color.darker_gray), fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BossItem(context: Context, boss: BossTimer, now: Instant) {
        val drawableId = getBossDrawableId(context, boss.bossName)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.glass_white))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start,
                modifier = GlanceModifier.fillMaxSize()
            ) {
                if (drawableId != 0) {
                    Image(
                        provider = ImageProvider(drawableId),
                        contentDescription = boss.bossName,
                        modifier = GlanceModifier.size(30.dp).padding(end = 8.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.Horizontal.Start,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = boss.bossName.uppercase(),
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(android.R.color.holo_orange_light),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    val diffSeconds = boss.nextSpawnTime?.let {
                        try {
                            val spawnTime = Instant.parse(it)
                            (spawnTime - now).inWholeSeconds
                        } catch (e: Exception) { null }
                    }

                    val timeText = when {
                        diffSeconds == null -> "??"
                        diffSeconds < -60 -> "SPAWNED"
                        diffSeconds in -60..0 -> "NOW!"
                        else -> {
                            val hours = diffSeconds / 3600
                            val minutes = (diffSeconds % 3600) / 60
                            val seconds = diffSeconds % 60
                            
                            if (hours > 0) {
                                "${hours}h ${minutes}m ${seconds}s"
                            } else {
                                "${minutes}m ${seconds}s"
                            }
                        }
                    }

                    Text(
                        text = timeText,
                        style = TextStyle(
                            color = ColorProvider(android.R.color.white),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }

    private fun getBossDrawableId(context: Context, bossName: String): Int {
        val resourceName = bossName.trim().lowercase()
            .replace(" ", "_")
            .replace("'", "")
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
}

class BossWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BossWidget()
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        BossWidget.startTicker(context)
    }
}
