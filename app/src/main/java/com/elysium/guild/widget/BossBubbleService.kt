package com.elysium.guild.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.elysium.guild.R
import com.elysium.guild.models.BossTimer
import com.elysium.guild.models.GuildEvent
import com.elysium.guild.repository.BossTimersRepository
import com.elysium.guild.repository.EventsRepository
import com.elysium.guild.utils.Constants
import com.elysium.guild.utils.PreferenceManager
import com.elysium.guild.utils.UIUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.datetime.*

@AndroidEntryPoint
class BossBubbleService : Service() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var bossRepository: BossTimersRepository

    @Inject
    lateinit var eventsRepository: EventsRepository

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    
    private var closeView: View? = null
    private var closeParams: WindowManager.LayoutParams? = null
    private var isCloseViewVisible = false
    private var userManuallyHidden = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null
    private var currentBosses: List<BossTimer> = emptyList()
    private var currentEvents: List<GuildEvent> = emptyList()

    private var screenWidth = 0
    private var screenHeight = 0
    private var bubbleIcon: ImageView? = null
    private var timerLayout: View? = null
    private var timerListContainer: LinearLayout? = null
    
    private var lastStableX = 0
    private var lastStableY = -1
    private var isExpanded = false
    private var isAnimating = false
    private var currentAppTheme: Int = Constants.THEME_SYSTEM

    private lateinit var prefs: SharedPreferences

    companion object {
        const val ACTION_SHOW = "com.elysium.guild.SHOW_BUBBLE"
        const val ACTION_HIDE = "com.elysium.guild.HIDE_BUBBLE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                userManuallyHidden = false
                showFloatingView()
            }
            ACTION_HIDE -> hideFloatingView()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        updateScreenDimensions()
        
        lastStableX = prefs.getInt(Constants.KEY_BUBBLE_LAST_X, screenWidth)
        lastStableY = prefs.getInt(Constants.KEY_BUBBLE_LAST_Y, dpToPx(Constants.BUBBLE_INITIAL_Y_DP))
        
        startForegroundService()
        setupFloatingView()
        setupCloseView()
        observeDataChanges()
        observeThemeChanges()
        startPeriodicUpdates()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenDimensions()
        if (isExpanded) {
            params!!.width = WindowManager.LayoutParams.MATCH_PARENT
            params!!.height = WindowManager.LayoutParams.MATCH_PARENT
            updateViewLayoutSafely()
        } else {
            snapToEdge()
        }
        updateCloseViewPosition()
    }

    private fun updateScreenDimensions() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager?.defaultDisplay
        val size = Point()
        display?.getRealSize(size)
        screenWidth = size.x
        screenHeight = size.y
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.BUBBLE_NOTIFICATION_CHANNEL_ID,
                Constants.BUBBLE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, Constants.BUBBLE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Elysium Bubble Active")
            .setContentText("Boss & Event schedule overlay is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(Constants.BUBBLE_NOTIFICATION_ID, notification)
    }

    private fun setupCloseView() {
        closeView = LayoutInflater.from(this).inflate(R.layout.layout_bubble_close, null)
        
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        closeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dpToPx(50)
        }
        
        closeView?.visibility = View.GONE
        windowManager?.addView(closeView, closeParams)
    }

    private fun updateCloseViewPosition() {
        if (closeView != null && closeParams != null) {
            windowManager?.updateViewLayout(closeView, closeParams)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_boss_bubble, null)
        bubbleIcon = floatingView?.findViewById(R.id.bubble_icon)
        timerLayout = floatingView?.findViewById(R.id.timer_list_container)
        
        timerListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        (timerLayout as? LinearLayout)?.apply {
            removeAllViews()
            addView(timerListContainer)
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastStableX
            y = lastStableY
        }

        bubbleIcon?.setOnTouchListener(object : View.OnTouchListener {
            private var touchOffsetFromCenter = PointF()
            private var isMoving = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (isExpanded || isAnimating) return false
                
                val closeCenter = getCloseCenter()
                val iconWidth = v.width
                val iconHeight = v.height
                val padding = dpToPx(8)
                val centerOffsetInWindowX = padding + iconWidth / 2f
                val centerOffsetInWindowY = padding + iconHeight / 2f

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val location = IntArray(2)
                        v.getLocationOnScreen(location)
                        val currentCenterX = location[0] + iconWidth / 2f
                        val currentCenterY = location[1] + iconHeight / 2f
                        touchOffsetFromCenter.set(event.rawX - currentCenterX, event.rawY - currentCenterY)
                        isMoving = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val freeCenterX = event.rawX - touchOffsetFromCenter.x
                        val freeCenterY = event.rawY - touchOffsetFromCenter.y
                        
                        if (!isMoving) {
                            val currentLoc = IntArray(2)
                            v.getLocationOnScreen(currentLoc)
                            if (abs(event.rawX - (currentLoc[0] + iconWidth / 2f)) > Constants.BUBBLE_DRAG_THRESHOLD_PX || 
                                abs(event.rawY - (currentLoc[1] + iconHeight / 2f)) > Constants.BUBBLE_DRAG_THRESHOLD_PX) {
                                isMoving = true
                                showCloseView()
                            }
                        }

                        if (isMoving) {
                            params!!.x = (freeCenterX - centerOffsetInWindowX).toInt()
                            params!!.y = (freeCenterY - centerOffsetInWindowY).toInt()
                            
                            updateViewLayoutSafely()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isMoving) {
                            val freeCenterX = event.rawX - touchOffsetFromCenter.x
                            val freeCenterY = event.rawY - touchOffsetFromCenter.y
                            val dist = sqrt(
                                ((freeCenterX - closeCenter.x) * (freeCenterX - closeCenter.x) + 
                                 (freeCenterY - closeCenter.y) * (freeCenterY - closeCenter.y)).toDouble()
                            ).toFloat()

                            if (dist < dpToPx(80)) {
                                performCollapseAndHide()
                            } else {
                                hideCloseView()
                                snapToEdge()
                            }
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            v.performClick()
                        }
                        isMoving = false
                        return true
                    }
                }
                return false
            }
        })

        bubbleIcon?.setOnClickListener {
            if (!isAnimating) {
                toggleExpandedView()
            }
        }

        floatingView?.setOnTouchListener { _, event ->
            if (isExpanded && !isAnimating) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val hitRect = android.graphics.Rect()
                    timerLayout?.getGlobalVisibleRect(hitRect)
                    val iconRect = android.graphics.Rect()
                    bubbleIcon?.getGlobalVisibleRect(iconRect)
                    
                    if (!hitRect.contains(event.rawX.toInt(), event.rawY.toInt()) && 
                        !iconRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        toggleExpandedView()
                        return@setOnTouchListener true
                    }
                } else if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    toggleExpandedView()
                    return@setOnTouchListener true
                }
            }
            false
        }

        floatingView?.visibility = View.GONE
        windowManager?.addView(floatingView, params)
        
        floatingView?.post {
            snapToEdge()
        }
    }

    private fun getCloseCenter(): Point {
        val closeIcon = closeView?.findViewById<View>(R.id.close_icon)
        if (closeIcon != null && closeIcon.isAttachedToWindow) {
            val location = IntArray(2)
            closeIcon.getLocationOnScreen(location)
            return Point(
                location[0] + closeIcon.width / 2,
                location[1] + closeIcon.height / 2
            )
        }
        return Point(screenWidth / 2, screenHeight - dpToPx(130))
    }

    private fun showCloseView() {
        if (isCloseViewVisible) return
        isCloseViewVisible = true
        
        val closeIcon = closeView?.findViewById<View>(R.id.close_icon)
        closeIcon?.animate()?.cancel()
        closeIcon?.apply {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
            rotation = 0f
            translationY = 0f
        }

        closeView?.apply {
            animate()?.cancel()
            visibility = View.VISIBLE
            alpha = 1f
            translationY = 0f
        }
    }

    private fun hideCloseView() {
        if (!isCloseViewVisible) return
        isCloseViewVisible = false
        closeView?.apply {
            animate()?.cancel()
            visibility = View.GONE
        }
    }

    private fun performCollapseAndHide() {
        val closeIcon = closeView?.findViewById<View>(R.id.close_icon) ?: return
        closeIcon.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        userManuallyHidden = true
        hideFloatingView()
        hideCloseView()
    }

    private fun showFloatingView() {
        if (!userManuallyHidden) {
            params!!.x = lastStableX
            params!!.y = lastStableY
            updateViewLayoutSafely()
            floatingView?.visibility = View.VISIBLE
        }
    }

    private fun hideFloatingView() {
        if (isExpanded) {
            toggleExpandedView()
        }
        floatingView?.visibility = View.GONE
    }

    private fun snapToEdge() {
        val currentX = params!!.x
        val viewWidth = if (floatingView?.width ?: 0 > 0) floatingView!!.width else dpToPx(70)
        val targetX = if (currentX + viewWidth / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - viewWidth
        }

        val animator = ValueAnimator.ofInt(currentX, targetX)
        animator.addUpdateListener { animation ->
            if (floatingView != null && (floatingView!!.isAttachedToWindow || floatingView!!.parent != null)) {
                params!!.x = animation.animatedValue as Int
                updateViewLayoutSafely()
            }
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lastStableX = targetX
                lastStableY = params!!.y
                savePosition(lastStableX, lastStableY)
            }
        })
        animator.duration = Constants.BUBBLE_SNAP_ANIMATION_DURATION
        animator.interpolator = DecelerateInterpolator()
        animator.start()
        
        adjustLayoutForSide(targetX == 0)
    }

    private fun savePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(Constants.KEY_BUBBLE_LAST_X, x)
            .putInt(Constants.KEY_BUBBLE_LAST_Y, y)
            .apply()
    }

    private fun adjustLayoutForSide(isLeft: Boolean) {
        val rootLayout = floatingView as? LinearLayout ?: return
        val icon = bubbleIcon ?: return
        val timers = timerLayout ?: return
        
        detachFromParent(icon)
        detachFromParent(timers)
        rootLayout.removeAllViews()
        rootLayout.orientation = LinearLayout.HORIZONTAL
        rootLayout.gravity = Gravity.CENTER_VERTICAL
        
        if (isLeft) {
            rootLayout.addView(icon)
            rootLayout.addView(timers)
        } else {
            rootLayout.addView(timers)
            rootLayout.addView(icon)
        }
    }

    private fun toggleExpandedView() {
        val rootLayout = floatingView as? LinearLayout ?: return
        val icon = bubbleIcon ?: return
        val timers = timerLayout ?: return
        
        isExpanded = !isExpanded
        isAnimating = true

        if (isExpanded) {
            lastStableX = params!!.x
            lastStableY = params!!.y
            
            val iconWidth = if (icon.width > 0) icon.width else dpToPx(55)
            val targetXForIcon = screenWidth - iconWidth - dpToPx(16)
            val targetYForIcon = dpToPx(Constants.BUBBLE_EXPANDED_Y_DP)
            
            animateMovement(params!!.x, targetXForIcon, params!!.y, targetYForIcon) {
                floatingView?.visibility = View.INVISIBLE
                
                params!!.width = WindowManager.LayoutParams.MATCH_PARENT
                params!!.height = WindowManager.LayoutParams.MATCH_PARENT
                params!!.x = 0
                params!!.y = 0
                
                rootLayout.orientation = LinearLayout.VERTICAL
                rootLayout.gravity = Gravity.TOP
                
                detachFromParent(icon)
                detachFromParent(timers)
                rootLayout.removeAllViews()
                
                val headerLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = targetYForIcon
                    }
                    setPadding(0, 0, dpToPx(16), 0)
                }
                headerLayout.addView(icon)
                
                timers.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                timers.visibility = View.VISIBLE
                applyThemeToWindow()
                
                rootLayout.addView(headerLayout)
                rootLayout.addView(timers)
                
                updateViewLayoutSafely()
                updateTimerDisplay()
                
                floatingView?.post {
                    floatingView?.visibility = View.VISIBLE
                    isAnimating = false
                }
            }
        } else {
            floatingView?.visibility = View.INVISIBLE
            timers.visibility = View.GONE
            
            params!!.width = WindowManager.LayoutParams.WRAP_CONTENT
            params!!.height = WindowManager.LayoutParams.WRAP_CONTENT
            
            val iconWidth = if (icon.width > 0) icon.width else dpToPx(55)
            params!!.x = screenWidth - iconWidth - dpToPx(16)
            params!!.y = dpToPx(Constants.BUBBLE_EXPANDED_Y_DP)
            
            rootLayout.orientation = LinearLayout.HORIZONTAL
            rootLayout.gravity = Gravity.CENTER_VERTICAL
            
            detachFromParent(icon)
            detachFromParent(timers)
            
            timers.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(8)
            }
            
            adjustLayoutForSide(false)
            updateViewLayoutSafely()
            
            floatingView?.post {
                floatingView?.visibility = View.VISIBLE
                animateMovement(params!!.x, lastStableX, params!!.y, lastStableY) {
                    adjustLayoutForSide(lastStableX == 0)
                    isAnimating = false
                }
            }
        }
    }

    private fun animateMovement(fromX: Int, toX: Int, fromY: Int, toY: Int, onEnd: () -> Unit) {
        val xAnimator = ValueAnimator.ofInt(fromX, toX)
        val yAnimator = ValueAnimator.ofInt(fromY, toY)
        
        xAnimator.addUpdateListener { 
            params!!.x = it.animatedValue as Int
            updateViewLayoutSafely()
        }
        yAnimator.addUpdateListener { 
            params!!.y = it.animatedValue as Int
            updateViewLayoutSafely()
        }
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(xAnimator, yAnimator)
        animatorSet.duration = Constants.BUBBLE_SNAP_ANIMATION_DURATION
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
        })
        animatorSet.start()
    }

    private fun detachFromParent(view: View) {
        try {
            (view.parent as? ViewGroup)?.removeView(view)
        } catch (e: Exception) { }
    }

    private fun observeDataChanges() {
        serviceScope.launch {
            bossRepository.bossDataChanged.collectLatest {
                fetchData()
            }
        }
        fetchData()
    }

    private fun observeThemeChanges() {
        serviceScope.launch {
            preferenceManager.themeMode.collectLatest { mode ->
                currentAppTheme = mode
                if (isExpanded) {
                    applyThemeToWindow()
                    updateTimerDisplay()
                }
            }
        }
    }

    private fun isDarkMode(): Boolean {
        return when (currentAppTheme) {
            Constants.THEME_DARK -> true
            Constants.THEME_LIGHT -> false
            else -> {
                (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun applyThemeToWindow() {
        val timers = timerLayout ?: return
        val isDark = isDarkMode()
        
        timers.setBackgroundResource(R.drawable.bg_bubble_window)
        val drawable = timers.background as? GradientDrawable
        if (drawable != null) {
            val bgColor = if (isDark) Color.parseColor("#E60B0B1A") else Color.parseColor("#F2FFFFFF")
            val strokeColor = if (isDark) Color.parseColor("#40FFFFFF") else Color.parseColor("#40000000")
            drawable.setColor(bgColor)
            drawable.setStroke(dpToPx(1), strokeColor)
        }
    }

    private fun fetchData() {
        serviceScope.launch {
            try {
                currentBosses = bossRepository.getBossTimers()
                currentEvents = eventsRepository.getEvents()
                updateTimerDisplay()
            } catch (e: Exception) { }
        }
    }

    private fun startPeriodicUpdates() {
        updateJob = serviceScope.launch {
            while (isActive) {
                updateTimerDisplay()
                delay(1000L)
                if (System.currentTimeMillis() % 60000 < 1000) {
                    fetchData()
                }
            }
        }
    }

    private fun formatCountdown(diff: Long): String {
        if (diff <= 0) return Constants.LABEL_READY
        
        val h = diff / 3600
        val m = (diff % 3600) / 60
        val s = diff % 60
        
        return when {
            h > 0 -> String.format("%dh %02dm %02ds", h, m, s)
            m > 0 -> String.format("%02dm %02ds", m, s)
            else -> String.format("%02ds", s)
        }
    }

    private fun updateTimerDisplay() {
        if (!isExpanded) return
        val container = timerListContainer ?: return
        container.removeAllViews()

        val now = Clock.System.now()
        val tz = TimeZone.of("Asia/Manila")
        val today = now.toLocalDateTime(tz).date

        val todayBosses = currentBosses.filter { boss ->
            boss.nextSpawnTime?.let {
                try {
                    val spawnDate = Instant.parse(it).toLocalDateTime(tz).date
                    spawnDate == today
                } catch (e: Exception) { false }
            } ?: false
        }.map { boss ->
            val diff = (Instant.parse(boss.nextSpawnTime!!) - now).inWholeSeconds
            Triple(boss.bossName, diff, true)
        }

        val todayEvents = currentEvents.filter { event ->
            try {
                val eventDate = Instant.parse(event.startTime).toLocalDateTime(tz).date
                eventDate == today
            } catch (e: Exception) { false }
        }.map { event ->
            val diff = (Instant.parse(event.startTime) - now).inWholeSeconds
            Triple(event.name, diff, false)
        }

        val allItems = (todayBosses + todayEvents)
            .filter { it.second > Constants.BUBBLE_SPAWNED_GRACE_PERIOD_SECONDS }
            .sortedBy { if (it.second > 0) it.second else Long.MAX_VALUE }

        val isDark = isDarkMode()
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val subTextColor = if (isDark) Color.parseColor("#B0FFFFFF") else Color.parseColor("#B0000000")

        if (allItems.isEmpty()) {
            addTimerRow(container, "No activities today", textColor)
        } else {
            addTimerRow(container, "Today's Schedule", ContextCompat.getColor(this, R.color.primary), isHeader = true)
            addTimerRow(container, "Bosses and guild events", subTextColor, isSubtitle = true)
            
            allItems.take(Constants.BUBBLE_MAX_ITEMS).forEach { (name, diff, isBoss) ->
                val timeStr = formatCountdown(diff)

                val colorVal = if (isBoss) {
                    ContextCompat.getColor(this, UIUtils.getBubbleStatusColorRes(diff))
                } else {
                    if (diff <= 0) ContextCompat.getColor(this, R.color.success) else ContextCompat.getColor(this, R.color.secondary)
                }
                
                val fullText = "$name: $timeStr"
                addTimerRow(container, fullText, colorVal, boldSpanEnd = name.length)
            }
        }
    }

    private fun addTimerRow(container: LinearLayout, text: String, colorVal: Int, isHeader: Boolean = false, isSubtitle: Boolean = false, boldSpanEnd: Int = -1) {
        val tv = TextView(this).apply {
            if (boldSpanEnd > 0) {
                val spannable = SpannableStringBuilder(text)
                spannable.setSpan(StyleSpan(Typeface.BOLD), 0, boldSpanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                this.text = spannable
            } else {
                this.text = text
            }
            
            this.setTextColor(colorVal)
            
            val textSize = when {
                isHeader -> Constants.BUBBLE_TEXT_SIZE_TITLE_SP
                isSubtitle -> Constants.BUBBLE_TEXT_SIZE_SUBTITLE_SP
                else -> Constants.BUBBLE_TEXT_SIZE_ROW_SP
            }
            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            
            val topPadding = if (isHeader) 12 else 4
            val bottomPadding = if (isSubtitle) 12 else 4
            this.setPadding(dpToPx(Constants.BUBBLE_ROW_PADDING_HORIZONTAL_DP), dpToPx(topPadding), dpToPx(Constants.BUBBLE_ROW_PADDING_HORIZONTAL_DP), dpToPx(bottomPadding))
            
            this.typeface = if (isHeader) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.gravity = if (isHeader || isSubtitle) Gravity.CENTER_HORIZONTAL else Gravity.START
            this.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(tv)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun updateViewLayoutSafely() {
        try {
            if (floatingView != null && (floatingView!!.isAttachedToWindow || floatingView!!.parent != null)) {
                windowManager?.updateViewLayout(floatingView, params)
            }
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (floatingView != null && floatingView?.parent != null) {
            windowManager?.removeView(floatingView)
        }
        if (closeView != null && closeView?.parent != null) {
            windowManager?.removeView(closeView)
        }
    }
}
