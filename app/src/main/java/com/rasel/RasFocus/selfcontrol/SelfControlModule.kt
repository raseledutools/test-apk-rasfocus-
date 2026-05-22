package com.rasel.RasFocus.selfcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.*

private val PrimaryBlue    = Color(0xFF4A6FE3)
private val DarkBlue       = Color(0xFF2E4BC6)
private val LightBlue      = Color(0xFF6B8EF5)
private val SoftBlue       = Color(0xFFDDE6FF)
private val AccentGreen    = Color(0xFF4CAF50)
private val SoftRed        = Color(0xFFFFEBEB)
private val RedAccent      = Color(0xFFE53935)
private val PurpleCard     = Color(0xFFE8D5F5)
private val OrangeCard     = Color(0xFFFFF3DC)
private val GrayBg         = Color(0xFFF2F4F8)
private val TextDark       = Color(0xFF1A1A2E)
private val TextGray       = Color(0xFF8A8A9A)
private val White          = Color.White
private val CardBlue       = Color(0xFF3A5FD4)
private val DarkerCardBlue = Color(0xFF2E4FBE)

// Premium Teal Colors
private val PremiumTealDark = Color(0xFF032220)
private val PremiumTealMid  = Color(0xFF08504B)
private val PremiumTealAccent = Color(0xFF14C3B2)

private data class InstalledAppInfo(val packageName: String, val appName: String, val icon: Drawable? = null)

data class ButtonPhoneSession(
    val endTimeMs: Long,
    val unlockMode: BpUnlockMode,
    val parentPassword: String,
    val requireLongText: Boolean,
    val allowedPackages: List<String>,
    val allowedWebsites: List<String>,
    val blockInternet: Boolean
)

enum class BpUnlockMode { SELF, PARENTS }
private enum class BpScreen { SETUP, LOCK_DETAIL, ALLOW_LIST, LAUNCHER, UNLOCK }
private enum class BpLockMode { SELF_CONTROL, PARENTS_CONTROL, LONG_TEXT }
private enum class BpPhoneOption { COMPLETE, CUSTOMIZE }
val DEFAULT_ALLOWED_PACKAGES = listOf(
    "com.android.dialer",
    "com.google.android.dialer",
    "com.samsung.android.dialer",
    "com.android.mms",
    "com.google.android.apps.messaging",
    "com.samsung.android.messaging",
    "com.android.calculator2",
    "com.google.android.calculator",
    "com.samsung.android.calculator",
    "com.android.contacts",
    "com.google.android.contacts",
    "com.android.settings",
    "com.android.clock",
    "com.google.android.deskclock",
    "com.samsung.android.app.clockpackage"
)



// সব popular Chrome/browser package names
val CHROME_PACKAGES = setOf(
    "com.android.chrome",
    "com.chrome.beta",
    "com.chrome.dev",
    "com.chrome.canary",
    "com.sec.android.app.sbrowser",
    "org.mozilla.firefox",
    "com.microsoft.emmx",
    "com.opera.browser",
    "com.brave.browser"
)
private enum class BpAllowTab { APPS, WEBSITES }

private const val LONG_UNLOCK_TEXT =
    "Focus mode is essential for modern living. By limiting my access to digital distractions, " +
    "I am taking control of my attention and time. This conscious decision allows me to live more " +
    "deeply in the present moment, fostering meaningful connections with the world around me and " +
    "enhancing my personal productivity. I acknowledge that unlocking this phone before my planned " +
    "time is a compromise of my goals. Therefore, I am typing this message to consciously confirm " +
    "my desire to exit this focused state. By committing to this act of typing, I accept full " +
    "responsibility for my actions and understand that focus is a practice, not a destination. " +
    "I will continue to strive for balance in my digital life, using technology as a tool rather " +
    "than allowing it to master my schedule. This is my commitment to myself and my future " +
    "well-being, prioritizing presence over pixels, and real-world interactions over notifications."

object BpPrefs {
    private const val PREFS = "bp_session_prefs"

    fun save(context: Context, session: ButtonPhoneSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putLong("end_time", session.endTimeMs)
            putString("unlock_mode", session.unlockMode.name)
            putString("parent_pass", session.parentPassword)
            putBoolean("long_text", session.requireLongText)
            putStringSet("allowed_pkg", session.allowedPackages.toSet())
            putStringSet("allowed_web", session.allowedWebsites.toSet())
            putBoolean("block_net", session.blockInternet)
            putBoolean("active", true)
            apply()
        }
    }

    fun clear(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("active", false).apply()

    fun isActive(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getBoolean("active", false) &&
                System.currentTimeMillis() < p.getLong("end_time", 0L)
    }

    fun load(context: Context): ButtonPhoneSession? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("active", false)) return null
        return ButtonPhoneSession(
            endTimeMs       = p.getLong("end_time", 0L),
            unlockMode      = BpUnlockMode.valueOf(p.getString("unlock_mode", "SELF") ?: "SELF"),
            parentPassword  = p.getString("parent_pass", "") ?: "",
            requireLongText = p.getBoolean("long_text", true),
            allowedPackages = (p.getStringSet("allowed_pkg", emptySet()) ?: emptySet()).toList(),
            allowedWebsites = (p.getStringSet("allowed_web", emptySet()) ?: emptySet()).toList(),
            blockInternet   = p.getBoolean("block_net", false)
        )
    }
}

class ButtonPhoneBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (BpPrefs.isActive(context)) {
                // Service Restart
                val serviceIntent = Intent(context, BpAppBlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                // Launcher Start
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                if (launchIntent != null) context.startActivity(launchIntent)
            }
        }
    }
}

class BpAppBlockerService : Service() {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var defaultDialer: String? = null
    private var defaultSms: String? = null
    
    // Core apps that should always be allowed
    private val coreAllowedPackages = mutableSetOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.android.server.telecom",
        "com.rasel.RasFocus" // App itself must be allowed
    )

    override fun onCreate() {
        super.onCreate()
        
        // Dynamic fetch default Call and SMS app
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        defaultDialer = telecomManager?.defaultDialerPackage
        defaultSms = Telephony.Sms.getDefaultSmsPackage(this)
        
        defaultDialer?.let { coreAllowedPackages.add(it) }
        defaultSms?.let { coreAllowedPackages.add(it) }

        startForegroundServiceNotification()
        startMonitoring()
    }

    private fun getLauncherPackages(): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfos.map { it.activityInfo.packageName }
    }

    private fun startMonitoring() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        job = scope.launch {
            val homePackages = getLauncherPackages()
            var graceUntil = 0L          // block suspend করার সময়সীমা
            var prevTop = ""             // আগের iteration এ কোন app ছিল

            while (isActive) {
                delay(400)

                val session = BpPrefs.load(this@BpAppBlockerService)
                if (session == null || !BpPrefs.isActive(this@BpAppBlockerService)) {
                    stopSelf(); break
                }

                val allAllowed = coreAllowedPackages + session.allowedPackages.toSet()

                val now = System.currentTimeMillis()
                val events = usageStatsManager.queryEvents(now - 800, now)
                val ev = UsageEvents.Event()
                var topPkg = ""
                while (events.hasNextEvent()) {
                    events.getNextEvent(ev)
                    if (ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED) topPkg = ev.packageName
                }

                // নিজের app বা কিছু detect না হলে skip
                if (topPkg.isEmpty() || topPkg == packageName) continue

                val isAllowed = allAllowed.contains(topPkg)
                val isHome    = homePackages.contains(topPkg)

                when {
                    // allowed app এ নতুন করে গেলে grace দাও — home থেকে app এ যাওয়ার transition cover করতে
                    isAllowed && topPkg != prevTop -> {
                        graceUntil = now + 3000L
                        prevTop = topPkg
                    }
                    // home এ গেলেও grace দাও — user হয়তো allowed app open করতে যাচ্ছে
                    isHome && prevTop.let { allAllowed.contains(it) } -> {
                        graceUntil = now + 3000L
                        prevTop = topPkg
                    }
                    else -> prevTop = topPkg
                }

                // grace active থাকলে block করব না
                if (now < graceUntil) continue

                // block করো শুধু যদি না-allowed app এ থাকে এবং home ও না
                if (!isAllowed && !isHome) {
                    forceReturnToFocusApp()
                }
                // home এ দীর্ঘক্ষণ থাকলেও ফেরত পাঠাও
                if (isHome && now >= graceUntil) {
                    forceReturnToFocusApp()
                }
            }
        }
    }

    private fun forceReturnToFocusApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (intent != null) startActivity(intent)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "button_phone_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Focus Mode Active", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Focus Mode Active")
                .setContentText("Distractions are strictly blocked.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Focus Mode Active")
                .setContentText("Distractions are strictly blocked.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}


fun promptInternetPanel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(panelIntent) } catch (e: Exception) {}
        Toast.makeText(context, "Please turn off Wi-Fi & Mobile Data", Toast.LENGTH_LONG).show()
    } else {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try { context.startActivity(intent) } catch (e: Exception) {}
    }
}

fun killBackgroundProcesses(context: Context) {
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = am.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (processInfo.processName != context.packageName) {
                    am.killBackgroundProcesses(processInfo.processName)
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
}

class SelfFocusAccessibilityService : AccessibilityService() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private val blockedKeywords = listOf("reels", "shorts", "tiktok")

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.i("SelfFocus", "SelfFocusAccessibilityService Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val blockingPrefs = getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)
        val isBlockingActive = blockingPrefs.getBoolean("is_blocking_active", false)
        if (!isBlockingActive) return
        val prefs = getSharedPreferences("rasfocus_prefs", Context.MODE_PRIVATE)
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val isStrictMode = prefs.getBoolean("strict_mode", false)
                if (isStrictMode && packageName != this.packageName) {
                    if (packageName.contains("packageinstaller")) {
                        val nodeText = collectNodeText(rootInActiveWindow).lowercase()
                        if (nodeText.contains("rasfocus") && nodeText.contains("uninstall")) {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                            Toast.makeText(this, "Strict Mode is ON!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val isKeywordsEnabled = prefs.getBoolean("keywords_enabled", false)
                if (isKeywordsEnabled) {
                    val nodeText = collectNodeText(rootInActiveWindow).lowercase()
                    if (blockedKeywords.any { nodeText.contains(it) }) {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        Toast.makeText(this, "Distracting Keyword Blocked!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    private fun collectNodeText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        if (node.text != null) sb.append(node.text).append(" ")
        if (node.contentDescription != null) sb.append(node.contentDescription).append(" ")
        for (i in 0 until node.childCount) sb.append(collectNodeText(node.getChild(i)))
        return sb.toString()
    }

    private fun showBlockedOverlay(appName: String, message: String) {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#E62E4BC6"))
            addView(TextView(this@SelfFocusAccessibilityService).apply {
                text = "🧘\n\n$message"
                textSize = 24f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(android.graphics.Color.WHITE)
                setPadding(64, 64, 64, 64)
            })
            val btn = android.widget.Button(this@SelfFocusAccessibilityService).apply {
                text = "Take a Deep Breath & Go Back"
                setOnClickListener { removeOverlay(); performGlobalAction(GLOBAL_ACTION_HOME) }
            }
            addView(btn)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        try { windowManager?.addView(layout, params); overlayView = layout } catch (e: Exception) {}
    }

    private fun removeOverlay() {
        overlayView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {}; overlayView = null }
    }
}

class SelfControlViewModel : ViewModel() {
    private val _keywordsEnabled = MutableStateFlow(true)
    val keywordsEnabled: StateFlow<Boolean> = _keywordsEnabled.asStateFlow()

    fun toggleKeywords(enabled: Boolean, context: Context) {
        _keywordsEnabled.update { enabled }
        context.getSharedPreferences("rasfocus_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("keywords_enabled", enabled).apply()
    }
}

@Composable
fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            try {
                drawable.toBitmap().asImageBitmap()
            } catch (e: Exception) {
                android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
            }
        }
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier.background(Color.Gray, CircleShape))
    }
}

@Composable
fun StayFocusedApp(
    navController: NavController,
    onSettingsClick: () -> Unit = {},
    viewModel: SelfControlViewModel = viewModel(),
    isComboMode: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    var bpSessionActive by remember { mutableStateOf(BpPrefs.isActive(context)) }

    if (bpSessionActive) {
        val session = BpPrefs.load(context)
        if (session != null) {
            BpLauncherScreen(
                session = session,
                onSessionEnd = { 
                    BpPrefs.clear(context)
                    bpSessionActive = false
                    context.stopService(Intent(context, BpAppBlockerService::class.java))
                }
            )
            return
        }
    }

    LaunchedEffect(Unit) { }

    MaterialTheme {
        if (isComboMode) {
            Box(Modifier.fillMaxSize().background(GrayBg)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    FocusLauncherCard(onSessionStart = { bpSessionActive = true })
                    Spacer(Modifier.height(16.dp))
                    ExtremBlockCard(onClick = { navController.navigate("extreme_block") })
                    Spacer(Modifier.height(16.dp))
                    PermissionBanner(context)
                    Spacer(Modifier.height(20.dp))
                    AnalyticsSection()
                    Spacer(Modifier.height(20.dp))
                    TakeABreakCard()
                    Spacer(Modifier.height(16.dp))
                    NormalModeCard()
                    Spacer(Modifier.height(20.dp))
                    QuickActionsSection(viewModel, navController, context)
                    Spacer(Modifier.height(20.dp))
                    ProfileTemplatesSection(navController)
                    Spacer(Modifier.height(20.dp))
                }
            }
        } else {
            Box(Modifier.fillMaxSize().background(GrayBg)) {
                Column(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                TopHeader(navController)
                                Spacer(Modifier.height(16.dp))
                                FocusLauncherCard(onSessionStart = { bpSessionActive = true })
                                Spacer(Modifier.height(16.dp))
                                ExtremBlockCard(onClick = { navController.navigate("extreme_block") })
                                Spacer(Modifier.height(16.dp))
                                PermissionBanner(context)
                                Spacer(Modifier.height(20.dp))
                                AnalyticsSection()
                                Spacer(Modifier.height(20.dp))
                                TakeABreakCard()
                                Spacer(Modifier.height(16.dp))
                                NormalModeCard()
                                Spacer(Modifier.height(20.dp))
                                QuickActionsSection(viewModel, navController, context)
                                Spacer(Modifier.height(20.dp))
                                ProfileTemplatesSection(navController)
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                        1 -> {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                TopHeader(navController)
                                Spacer(Modifier.height(16.dp))
                                FocusLauncherCard(onSessionStart = { bpSessionActive = true })
                                Spacer(Modifier.height(16.dp))
                                ExtremBlockCard(onClick = { navController.navigate("extreme_block") })
                                Spacer(Modifier.height(16.dp))
                                NormalModeCard()
                                Spacer(Modifier.height(16.dp))
                                TakeABreakCard()
                                Spacer(Modifier.height(20.dp))
                                ProfileTemplatesSection(navController)
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                        2 -> {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                TopHeader()
                                Spacer(Modifier.height(16.dp))
                                AnalyticsSection()
                                Spacer(Modifier.height(16.dp))
                                QuickActionsSection(viewModel, navController, context)
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                        3 -> {
                            LaunchedEffect(Unit) { onSettingsClick() }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                TopHeader()
                                Spacer(Modifier.height(20.dp))
                                AccountSection(context)
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                    SelfControlBottomNav(selectedTab) { selectedTab = it }
                }
            }
        }
    }
}

@Composable
fun TopHeader(navController: NavController? = null) {
    Column {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(DarkBlue, LightBlue)),
                    RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(46.dp).background(White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = White, modifier = Modifier.size(22.dp))
                }
                Box(Modifier.background(AccentGreen, RoundedCornerShape(50.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💎", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("5 days free!", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Box(Modifier.size(46.dp).background(White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications",
                        tint = White, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 72.dp), horizontalAlignment = Alignment.Start) {
                Text("Welcome", color = White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("Stay Focused", color = White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            }
        }

        // Adult Block + Deep Study quick-access buttons
        if (navController != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Adult Block Button
                Card(
                    modifier = Modifier.weight(1f).clickable { navController.navigate("adult_block") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D0059)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF6A0DAD), Color(0xFF2D0059))))
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Box(
                                Modifier.size(44.dp).background(White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFF6BFF), modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Adult Block", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("100% Safe Browsing", color = White.copy(alpha = 0.65f), fontSize = 11.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.background(Color(0xFFFF6BFF).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(6.dp).background(Color(0xFF00FF88), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("Tap to Enable", color = Color(0xFFFF6BFF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Deep Study Button
                Card(
                    modifier = Modifier.weight(1f).clickable { navController.navigate("deep_study") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF001A0A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF005C3B), Color(0xFF001A0A))))
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Box(
                                Modifier.size(44.dp).background(White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF00FFB2), modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Deep Study", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Full Focus Mode", color = White.copy(alpha = 0.65f), fontSize = 11.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.background(Color(0xFF00FFB2).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(6.dp).background(Color(0xFF00FFB2), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("Start Session", color = Color(0xFF00FFB2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBanner(context: Context) {
    val isAccessibilityOn = remember {
        try {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            enabled.contains(context.packageName, ignoreCase = true)
        } catch (e: Exception) { false }
    }

    val needsBatteryFix = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName).not()
        } else {
            false
        }
    }

    if (isAccessibilityOn && !needsBatteryFix) return

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.fillMaxWidth().background(SoftRed, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Text("Grant the following permissions for Stay Focused to work properly!",
                    fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))

            if (!isAccessibilityOn) {
                PermissionRow(
                    icon = Icons.Default.Accessibility,
                    label = "Accessibility\nPermission",
                    buttonLabel = "Enable",
                    buttonColor = PrimaryBlue,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        Toast.makeText(context, "Turn on RasFocus+ Accessibility", Toast.LENGTH_LONG).show()
                    }
                )
                if (needsBatteryFix) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = GrayBg, thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (needsBatteryFix) {
                PermissionRow(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Battery Optimisation",
                    buttonLabel = "Disable",
                    buttonColor = PrimaryBlue,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionRow(icon: ImageVector, label: String, buttonLabel: String, buttonColor: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextDark, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
        }
        Button(onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(50.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            Text(buttonLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AnalyticsSection() {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Analytics", color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsCard(modifier = Modifier.weight(1f), icon = "⏰", label = "Screen Time", value = "01 hrs 19 mins", change = "-66 percent", positive = true)
            AnalyticsCard(modifier = Modifier.weight(1f), icon = "🚀", label = "App Launches", value = "129", change = "-336 launches", positive = true)
        }
    }
}

@Composable
fun AnalyticsCard(modifier: Modifier, icon: String, label: String, value: String, change: String, positive: Boolean) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = TextGray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(4.dp))
            Text(change, fontSize = 12.sp, color = if (positive) AccentGreen else RedAccent, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View", fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextDark)
            }
        }
    }
}

@Composable
fun TakeABreakCard() {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PurpleCard)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("☕", fontSize = 28.sp)
                Spacer(Modifier.height(8.dp))
                Text("Take a Break", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Spacer(Modifier.height(4.dp))
                Text("Take a break from your phone and focus on things that really matter.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun NormalModeCard() {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OrangeCard)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(22.dp), tint = TextDark)
                    Spacer(Modifier.width(10.dp))
                    Text("Strict Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Level", fontSize = 11.sp, color = TextGray)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = RedAccent)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Cannot disable Accessibility or uninstall the app during active sessions.",
                fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun QuickActionsSection(viewModel: SelfControlViewModel, navController: NavController, context: Context) {
    val keywordsEnabled by viewModel.keywordsEnabled.collectAsState()

    // SharedPreferences থেকে real blocked apps ও sites count পড়া
    val appsBlockedCount = remember {
        context.getSharedPreferences("rasfocus_prefs", Context.MODE_PRIVATE)
            .getStringSet("blocked_apps", emptySet())?.size ?: 0
    }
    val sitesBlockedCount = remember {
        context.getSharedPreferences("rasfocus_prefs", Context.MODE_PRIVATE)
            .getStringSet("blocked_sites", emptySet())?.size ?: 0
    }

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Quick Actions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextGray)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue)) {
            Column {
                QuickActionRow(icon = Icons.Default.MobileOff, label = "Apps Blocked",
                    value = appsBlockedCount.toString(), bgColor = CardBlue, divider = true,
                    onClick = { navController.navigate("single_apps") })
                QuickActionRow(icon = Icons.Default.DesktopWindows, label = "Sites Blocked",
                    value = sitesBlockedCount.toString(), bgColor = DarkerCardBlue, divider = true,
                    onClick = { navController.navigate("adult_block") })
                QuickActionRow(icon = Icons.Default.Schedule, label = "Schedule Blocks",
                    value = "Profiles", bgColor = CardBlue.copy(alpha = 0.85f), divider = true,
                    onClick = { navController.navigate("schedule_blocks") })
                QuickActionRow(icon = Icons.Default.Shield, label = "Adult Block",
                    value = "Safe", bgColor = DarkerCardBlue.copy(alpha = 0.9f), divider = true,
                    onClick = { navController.navigate("adult_block") })
                Row(Modifier.fillMaxWidth().background(CardBlue.copy(alpha = 0.6f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center) {
                        Text("A|", fontSize = 18.sp, color = White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (keywordsEnabled) "Active" else "Inactive", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Keywords Blocked (Shorts/Reels)", color = White.copy(alpha = 0.75f), fontSize = 13.sp)
                    }
                    Switch(checked = keywordsEnabled,
                        onCheckedChange = { viewModel.toggleKeywords(it, context) },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = AccentGreen,
                            uncheckedThumbColor = White, uncheckedTrackColor = White.copy(alpha = 0.3f)))
                }
            }
        }
    }
}

@Composable
fun QuickActionRow(icon: ImageVector, label: String, value: String, bgColor: Color, divider: Boolean, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(bgColor).clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(value, color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(label, color = White.copy(alpha = 0.75f), fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
        }
        if (divider) HorizontalDivider(color = White.copy(alpha = 0.1f), thickness = 1.dp)
    }
}

@Composable
fun ProfileTemplatesSection(navController: NavController) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Profile Templates", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
        Spacer(Modifier.height(4.dp))
        Text("Tap to start creating a profile with these presets", fontSize = 13.sp, color = TextGray)
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TemplateCard("💼", "Work Focus", "9am – 5pm", "Block apps during work hours to focus deeply.", SoftBlue, onClick = { navController.navigate("schedule_blocks") }) }
            item { TemplateCard("⏰", "Social Limit", "Every day · 30 min limit", "Cap your social media time to just half an hour a day.", SoftRed, badgeText = "Blocklist", badgeColor = RedAccent, onClick = { navController.navigate("schedule_blocks") }) }
            item { TemplateCard("🌙", "Night Mode", "10pm – 7am", "Wind down and improve sleep quality.", PurpleCard, onClick = { navController.navigate("schedule_blocks") }) }
        }
    }
}

@Composable
fun TemplateCard(emoji: String, title: String, subtitle: String, detail: String, bgColor: Color, badgeText: String? = null, badgeColor: Color = RedAccent, onClick: () -> Unit) {
    Card(Modifier.width(200.dp).clickable { onClick() }, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(emoji, fontSize = 26.sp)
                Box(Modifier.size(32.dp).background(White.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextDark)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = TextDark.copy(alpha = 0.7f))
            if (badgeText != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(badgeText, color = badgeColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(detail, fontSize = 12.sp, color = TextDark.copy(alpha = 0.65f), lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SelfControlBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
        Triple("Modes", Icons.Default.FlashOn, Icons.Outlined.FlashOn),
        Triple("Analytics", Icons.Default.BarChart, Icons.Outlined.BarChart),
        Triple("Account", Icons.Default.Person, Icons.Outlined.Person)
    )
    NavigationBar(containerColor = White, tonalElevation = 8.dp) {
        items.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
            NavigationBarItem(
                selected = selected == index, onClick = { onSelect(index) },
                icon = {
                    if (selected == index) {
                        Box(Modifier.background(SoftBlue, RoundedCornerShape(50.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Icon(filledIcon, contentDescription = label, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                    } else { Icon(outlinedIcon, contentDescription = label, tint = TextGray, modifier = Modifier.size(22.dp)) }
                },
                label = { Text(label, fontSize = 11.sp, color = if (selected == index) PrimaryBlue else TextGray, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun AccountSection(context: Context) {
    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (e: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "1.0"

    Column(Modifier.padding(horizontal = 20.dp)) {
        // Profile Card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).background(
                        Brush.verticalGradient(listOf(PrimaryBlue, DarkBlue)), CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("RasFocus User", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Stay Focused · v$versionName", fontSize = 13.sp, color = TextGray)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().background(SoftBlue, RoundedCornerShape(12.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("5", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryBlue)
                        Text("Days Free", fontSize = 11.sp, color = TextGray)
                    }
                    Box(Modifier.width(1.dp).height(36.dp).background(TextGray.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentGreen)
                        Text("Plan", fontSize = 11.sp, color = TextGray)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Settings Options
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column {
                AccountRow(
                    icon = Icons.Default.Shield,
                    label = "Privacy & Security",
                    tint = PrimaryBlue,
                    onClick = {}
                )
                HorizontalDivider(color = GrayBg)
                AccountRow(
                    icon = Icons.Default.Notifications,
                    label = "Notifications",
                    tint = AccentGreen,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                HorizontalDivider(color = GrayBg)
                AccountRow(
                    icon = Icons.Default.Accessibility,
                    label = "Accessibility Permission",
                    tint = PrimaryBlue,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                HorizontalDivider(color = GrayBg)
                AccountRow(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Battery Optimization",
                    tint = RedAccent,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                HorizontalDivider(color = GrayBg)
                AccountRow(
                    icon = Icons.Default.Info,
                    label = "App Version $versionName",
                    tint = TextGray,
                    showArrow = false,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
        if (showArrow) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ExtremBlockCard(onClick: () -> Unit) {
    // Dark red / crimson gradient — বোঝায় এটা সবচেয়ে কঠোর mode
    val gradientStart = Color(0xFF7B0000)
    val gradientEnd   = Color(0xFFB71C1C)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(listOf(gradientStart, gradientEnd)),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Extreme Block",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "MAX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "সর্বোচ্চ blocking — Adult, Reels, Apps & Protection",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FocusLauncherCard(onSessionStart: () -> Unit) {
    var showSetup by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { showSetup = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumTealMid),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Color.White, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhoneLocked, contentDescription = null, tint = PremiumTealDark, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Button Phone Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text("Lock yourself to minimal apps only", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
        }
    }

    if (showSetup) {
        BpSetupDialog(
            onDismiss = { showSetup = false },
            onSessionStart = { showSetup = false; onSessionStart() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BpSetupDialog(onDismiss: () -> Unit, onSessionStart: () -> Unit) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(BpScreen.SETUP) }
    var lockMode by remember { mutableStateOf(BpLockMode.SELF_CONTROL) }
    var phoneOption by remember { mutableStateOf(BpPhoneOption.COMPLETE) }
    var lockModeExpanded by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf("0") }
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("30") }
    var parentPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var blockInternet by remember { mutableStateOf(false) }
    var allowedPkgs by remember { mutableStateOf(DEFAULT_ALLOWED_PACKAGES.toSet()) }
    var allowedWebs by remember { mutableStateOf(setOf<String>()) }

    // helper to build unlockMode & requireLongText from lockMode
    val unlockMode by remember { derivedStateOf {
        when (lockMode) {
            BpLockMode.PARENTS_CONTROL -> BpUnlockMode.PARENTS
            else -> BpUnlockMode.SELF
        }
    }}
    val requireLongText by remember { derivedStateOf { lockMode == BpLockMode.LONG_TEXT } }

    Dialog(
        onDismissRequest = { if (screen == BpScreen.SETUP) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = screen == BpScreen.SETUP)
    ) {
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                BpScreen.SETUP -> Column(
                    Modifier.fillMaxSize().background(Color.White)
                        .verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PremiumTealDark)
                        }
                        Text("Button Phone Setup", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PremiumTealDark)
                    }
                    Spacer(Modifier.height(24.dp))

                    // Phone Option
                    Text("Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumTealDark)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Complete Button Phone
                        Card(
                            modifier = Modifier.weight(1f).clickable { phoneOption = BpPhoneOption.COMPLETE },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (phoneOption == BpPhoneOption.COMPLETE) PremiumTealMid else GrayBg
                            ),
                            elevation = CardDefaults.cardElevation(if (phoneOption == BpPhoneOption.COMPLETE) 4.dp else 0.dp)
                        ) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhoneLocked,
                                    contentDescription = null,
                                    tint = if (phoneOption == BpPhoneOption.COMPLETE) Color.White else PremiumTealDark,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Complete\nButton Phone",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (phoneOption == BpPhoneOption.COMPLETE) Color.White else PremiumTealDark,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "শুধু essential apps",
                                    fontSize = 10.sp,
                                    color = if (phoneOption == BpPhoneOption.COMPLETE) Color.White.copy(alpha = 0.75f) else TextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // Customize Button
                        Card(
                            modifier = Modifier.weight(1f).clickable { phoneOption = BpPhoneOption.CUSTOMIZE },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (phoneOption == BpPhoneOption.CUSTOMIZE) PremiumTealMid else GrayBg
                            ),
                            elevation = CardDefaults.cardElevation(if (phoneOption == BpPhoneOption.CUSTOMIZE) 4.dp else 0.dp)
                        ) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (phoneOption == BpPhoneOption.CUSTOMIZE) Color.White else PremiumTealDark,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Customize\nButton",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (phoneOption == BpPhoneOption.CUSTOMIZE) Color.White else PremiumTealDark,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Apps নিজে বেছে নাও",
                                    fontSize = 10.sp,
                                    color = if (phoneOption == BpPhoneOption.CUSTOMIZE) Color.White.copy(alpha = 0.75f) else TextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Lock Mode Dropdown
                    Text("Lock Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumTealDark)
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = lockModeExpanded,
                        onExpandedChange = { lockModeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (lockMode) {
                                BpLockMode.SELF_CONTROL -> "Self Control"
                                BpLockMode.PARENTS_CONTROL -> "Parents Control"
                                BpLockMode.LONG_TEXT -> "Long Text"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unlock type select করুন") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lockModeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealAccent)
                        )
                        ExposedDropdownMenu(
                            expanded = lockModeExpanded,
                            onDismissRequest = { lockModeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Column { Text("Self Control", fontWeight = FontWeight.SemiBold); Text("নিজেই unlock করতে পারবে (time শেষে)", fontSize = 11.sp, color = TextGray) } },
                                onClick = { lockMode = BpLockMode.SELF_CONTROL; lockModeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Column { Text("Parents Control", fontWeight = FontWeight.SemiBold); Text("শুধু parents password দিয়ে unlock", fontSize = 11.sp, color = TextGray) } },
                                onClick = { lockMode = BpLockMode.PARENTS_CONTROL; lockModeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Column { Text("Long Text", fontWeight = FontWeight.SemiBold); Text("~200 words টাইপ করলে unlock হবে", fontSize = 11.sp, color = TextGray) } },
                                onClick = { lockMode = BpLockMode.LONG_TEXT; lockModeExpanded = false }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Extra options
                    Text("Extra Options", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumTealDark)
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GrayBg)) {
                        Row(Modifier.fillMaxWidth().clickable { blockInternet = !blockInternet }
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = PremiumTealDark, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Block Internet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PremiumTealDark)
                                Text("Session চলাকালীন internet বন্ধ থাকবে", fontSize = 11.sp, color = TextGray)
                            }
                            Switch(checked = blockInternet, onCheckedChange = { blockInternet = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = PremiumTealAccent))
                        }
                    }
                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = { screen = BpScreen.LOCK_DETAIL },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTealMid)
                    ) {
                        Text("Next →", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }

                BpScreen.LOCK_DETAIL -> Column(
                    Modifier.fillMaxSize().background(Color.White)
                        .verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { screen = BpScreen.SETUP }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PremiumTealDark)
                        }
                        Text(
                            when (lockMode) {
                                BpLockMode.SELF_CONTROL -> "Set Duration"
                                BpLockMode.PARENTS_CONTROL -> "Set Password"
                                BpLockMode.LONG_TEXT -> "Long Text Mode"
                            },
                            fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PremiumTealDark
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    when (lockMode) {
                        BpLockMode.SELF_CONTROL -> {
                            Text("Duration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumTealDark)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(days, { days = it }, label = { Text("Days") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealAccent))
                                OutlinedTextField(hours, { hours = it }, label = { Text("Hours") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealAccent))
                                OutlinedTextField(minutes, { minutes = it }, label = { Text("Mins") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealAccent))
                            }
                        }
                        BpLockMode.PARENTS_CONTROL -> {
                            Text("Unlock Password", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumTealDark)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(parentPass, { parentPass = it }, label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealAccent))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(confirmPass, { confirmPass = it }, label = { Text("Confirm Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealAccent))
                        }
                        BpLockMode.LONG_TEXT -> {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PremiumTealMid.copy(alpha = 0.08f))) {
                                Column(Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EditNote, contentDescription = null, tint = PremiumTealMid, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Long Text Unlock Active", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PremiumTealDark)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("Unlock করতে চাইলে ~200 words এর একটি paragraph টাইপ করতে হবে।", fontSize = 13.sp, color = TextGray, lineHeight = 20.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("এখন কোনো password বা time দিতে হবে না।", fontSize = 13.sp, color = PremiumTealMid, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (lockMode == BpLockMode.PARENTS_CONTROL && parentPass != confirmPass) {
                                return@Button
                            }
                            screen = BpScreen.ALLOW_LIST
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTealMid)
                    ) {
                        Text(
                            if (phoneOption == BpPhoneOption.COMPLETE) "Next: Review Apps →" else "Next: Choose Apps →",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White
                        )
                    }
                }

                BpScreen.ALLOW_LIST -> BpAllowListScreen(
                    selectedPkgs = allowedPkgs,
                    selectedWebs = allowedWebs,
                    isComplete = phoneOption == BpPhoneOption.COMPLETE,
                    onPkgsChanged = { allowedPkgs = it },
                    onWebsChanged = { allowedWebs = it },
                    onBack = { screen = BpScreen.LOCK_DETAIL },
                    onStart = {
                        if (!hasUsageStatsPermission(context)) {
                            Toast.makeText(context, "Please enable Usage Access", Toast.LENGTH_SHORT).show()
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            return@BpAllowListScreen
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
                            val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(overlayIntent)
                            return@BpAllowListScreen
                        }

                        val totalMs =
                            (days.toLongOrNull() ?: 0L) * 86_400_000L +
                            (hours.toLongOrNull() ?: 0L) * 3_600_000L +
                            (minutes.toLongOrNull() ?: 30L) * 60_000L

                        val session = ButtonPhoneSession(
                            endTimeMs       = System.currentTimeMillis() + totalMs,
                            unlockMode      = unlockMode,
                            parentPassword  = parentPass,
                            requireLongText = requireLongText,
                            allowedPackages = (if (phoneOption == BpPhoneOption.COMPLETE) DEFAULT_ALLOWED_PACKAGES else allowedPkgs.toList()),
                            allowedWebsites = allowedWebs.toList(),
                            blockInternet   = blockInternet
                        )
                        
                        if (blockInternet) promptInternetPanel(context)
                        killBackgroundProcesses(context)
                        
                        BpPrefs.save(context, session)

                        val serviceIntent = Intent(context, BpAppBlockerService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        onSessionStart()
                    }
                )

                else -> {}
            }
        }
    }
}

@Composable
private fun BpAllowListScreen(
    selectedPkgs: Set<String>,
    selectedWebs: Set<String>,
    isComplete: Boolean = false,
    onPkgsChanged: (Set<String>) -> Unit,
    onWebsChanged: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(BpAllowTab.APPS) }
    var webInput by remember { mutableStateOf("") }

    val installedApps = remember {
        val pm = context.packageManager
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val dialerPkg = telecomManager?.defaultDialerPackage
        val smsPkg = Telephony.Sms.getDefaultSmsPackage(context)
        
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 ||
                it.packageName == dialerPkg || it.packageName == smsPkg
            }
            .map { InstalledAppInfo(it.packageName, pm.getApplicationLabel(it).toString(), pm.getApplicationIcon(it)) }
            .sortedBy { it.appName }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(PremiumTealMid, PremiumTealDark)))
            .padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Allow List", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text(
                        if (isComplete) "Default apps fixed — শুধু এরাই কাজ করবে" else "Apps/sites বেছে নাও",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().background(GrayBg).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BpTabBtn("📱 Apps (${selectedPkgs.size})", activeTab == BpAllowTab.APPS, Modifier.weight(1f)) { activeTab = BpAllowTab.APPS }
            BpTabBtn("🌐 Websites (${selectedWebs.size})", activeTab == BpAllowTab.WEBSITES, Modifier.weight(1f)) { activeTab = BpAllowTab.WEBSITES }
        }

        when (activeTab) {
            BpAllowTab.APPS -> LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                items(installedApps) { app ->
                    val selected = app.packageName in selectedPkgs
                    val isDefaultApp = app.packageName in DEFAULT_ALLOWED_PACKAGES
                    val isLocked = isComplete || (isDefaultApp)
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).then(
                            if (!isLocked) Modifier.clickable {
                                onPkgsChanged(if (selected) selectedPkgs - app.packageName else selectedPkgs + app.packageName)
                            } else Modifier
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selected) PremiumTealMid.copy(alpha = 0.1f) else White),
                        elevation = CardDefaults.cardElevation(if (selected) 2.dp else 1.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIconImage(drawable = app.icon, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.appName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                    color = if (isLocked && !selected) TextGray else PremiumTealDark)
                                Text(app.packageName, fontSize = 10.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (isLocked) {
                                Icon(
                                    if (selected) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (selected) PremiumTealMid else TextGray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (selected) PremiumTealMid else TextGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            BpAllowTab.WEBSITES -> Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = webInput, onValueChange = { webInput = it },
                        label = { Text("Website (e.g. google.com)") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTealMid)
                    )
                    Button(
                        onClick = {
                            val site = webInput.trim().lowercase()
                                .removePrefix("https://").removePrefix("http://").removePrefix("www.")
                            if (site.isNotEmpty()) { onWebsChanged(selectedWebs + site); webInput = "" }
                        },
                        modifier = Modifier.height(56.dp).align(Alignment.CenterVertically),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTealMid)
                    ) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
                }
                if (selectedWebs.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌐", fontSize = 40.sp); Spacer(Modifier.height(8.dp))
                            Text("কোনো website add করা হয়নি", color = TextGray)
                            Text("উপরে লিখে + চাপুন", fontSize = 12.sp, color = TextGray)
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        items(selectedWebs.toList()) { site ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PremiumTealMid.copy(alpha=0.1f))) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = PremiumTealMid, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(site, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = PremiumTealDark)
                                    IconButton(onClick = { onWebsChanged(selectedWebs - site) }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = RedAccent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().background(White).padding(16.dp)) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTealDark)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("START FOCUS SESSION", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun BpTabBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(42.dp), shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PremiumTealMid else White,
            contentColor = if (selected) White else TextDark),
        elevation = ButtonDefaults.buttonElevation(if (selected) 4.dp else 0.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun launchApp(context: Context, packageName: String) {
    val pm = context.packageManager
    // 1. Try standard launch intent first
    val launchIntent = pm.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(launchIntent); return } catch (e: Exception) {}
    }
    // 2. Phone dialer — use ACTION_DIAL
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    if (packageName == telecomManager?.defaultDialerPackage) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(dialIntent); return
        } catch (e: Exception) {}
    }
    // 3. SMS / Messaging — use ACTION_MAIN with SMS category
    val smsPkg = android.provider.Telephony.Sms.getDefaultSmsPackage(context)
    if (packageName == smsPkg) {
        try {
            val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent); return
        } catch (e: Exception) {}
    }
    // 4. Fallback — query all activities of that package and launch first one
    try {
        val mainIntent = Intent(Intent.ACTION_MAIN).apply { setPackage(packageName) }
        val activities = pm.queryIntentActivities(mainIntent, 0)
        if (activities.isNotEmpty()) {
            val actInfo = activities[0].activityInfo
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(actInfo.packageName, actInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent); return
        }
    } catch (e: Exception) {}
    // 5. Last resort — open app in Play Store / system info
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {}
}

@Composable
internal fun BpLauncherScreen(session: ButtonPhoneSession, onSessionEnd: () -> Unit) {
    var showUnlock by remember { mutableStateOf(false) }
    var showAppsSheet by remember { mutableStateOf(false) }
    var showWebsSheet by remember { mutableStateOf(false) }
    var timeLeftMs by remember { mutableStateOf(session.endTimeMs - System.currentTimeMillis()) }
    val context = LocalContext.current

    // Live clock
    var clockTime by remember { mutableStateOf(
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    )}
    var clockDate by remember { mutableStateOf(
        java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.getDefault()).format(java.util.Date())
    )}

    BackHandler(enabled = true) { /* ignore */ }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            timeLeftMs = session.endTimeMs - System.currentTimeMillis()
            clockTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            clockDate = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale.getDefault()).format(java.util.Date())
            if (timeLeftMs <= 0) { onSessionEnd(); break }
        }
    }

    val totalSecs = (timeLeftMs / 1_000).coerceAtLeast(0)
    val hoursLeft = totalSecs / 3_600
    val minsLeft  = (totalSecs % 3_600) / 60
    val secsLeft  = totalSecs % 60
    val daysLeft  = totalSecs / 86_400

    // Build allowed apps info
    val allowedAppsInfo = remember(session.allowedPackages) {
        val pm = context.packageManager
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val dialerPkg = telecomManager?.defaultDialerPackage
        val smsPkg = android.provider.Telephony.Sms.getDefaultSmsPackage(context)
        val allPkgs = (listOfNotNull(dialerPkg, smsPkg) + session.allowedPackages).distinct()
        allPkgs.mapNotNull { pkg ->
            try { InstalledAppInfo(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(), pm.getApplicationIcon(pkg)) }
            catch (e: Exception) { null }
        }.distinctBy { it.packageName }
    }

    // Phone & Messages specifically
    val pm = context.packageManager
    val telecomMgr = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    val dialerPkg = remember { telecomMgr?.defaultDialerPackage }
    val smsPkg = remember { android.provider.Telephony.Sms.getDefaultSmsPackage(context) }
    val dialerInfo = remember(dialerPkg) { dialerPkg?.let { pkg -> try { InstalledAppInfo(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString(), pm.getApplicationIcon(pkg)) } catch(e:Exception){null} } }
    val smsInfo = remember(smsPkg) { smsPkg?.let { pkg -> try { InstalledAppInfo(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString(), pm.getApplicationIcon(pkg)) } catch(e:Exception){null} } }

    val chromeAllowed = CHROME_PACKAGES.any { it in session.allowedPackages }

    if (showUnlock) {
        BpUnlockScreen(session = session, onUnlocked = onSessionEnd, onBack = { showUnlock = false })
        return
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A1628))) {
        Column(Modifier.fillMaxSize()) {

            // ── Status Bar ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(clockTime, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // ── Big Clock + Date ─────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clockTime, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Thin, letterSpacing = (-2).sp)
                Text(clockDate, color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp, fontWeight = FontWeight.Normal)
            }

            // ── Focus Timer Badge ────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PremiumTealAccent, modifier = Modifier.size(14.dp))
                        Text(
                            if (daysLeft > 0) "%dd %02dh %02dm".format(daysLeft, hoursLeft % 24, minsLeft)
                            else "%02d:%02d:%02d left".format(hoursLeft, minsLeft, secsLeft),
                            color = PremiumTealAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Phone & Messages dock ────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dialerInfo?.let { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { launchApp(context, app.packageName) }) {
                        Box(
                            Modifier.size(72.dp)
                                .background(Color(0xFF1C6B3A), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIconImage(drawable = app.icon, modifier = Modifier.size(44.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(app.appName, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                smsInfo?.let { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { launchApp(context, app.packageName) }) {
                        Box(
                            Modifier.size(72.dp)
                                .background(Color(0xFF1A4A8A), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIconImage(drawable = app.icon, modifier = Modifier.size(44.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(app.appName, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Allowed Apps & Websites buttons ─────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Apps button
                Box(
                    Modifier.weight(1f)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable { showAppsSheet = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = PremiumTealAccent, modifier = Modifier.size(20.dp))
                        Text("Apps (${allowedAppsInfo.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Websites button
                if (session.allowedWebsites.isNotEmpty()) {
                    Box(
                        Modifier.weight(1f)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable { showWebsSheet = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = PremiumTealAccent, modifier = Modifier.size(20.dp))
                            Text("Sites (${session.allowedWebsites.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Unlock button ────────────────────────────────────────
            Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                TextButton(
                    onClick = { showUnlock = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Unlock", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                }
            }
        }

        // ── Apps Bottom Sheet ────────────────────────────────────────
        if (showAppsSheet) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showAppsSheet = false })
            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color(0xFF111827), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allowed Apps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showAppsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // App grid
                    val rows = allowedAppsInfo.chunked(4)
                    rows.forEach { rowApps ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowApps.forEach { app ->
                                Column(
                                    Modifier.weight(1f).clickable { launchApp(context, app.packageName); showAppsSheet = false },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(Modifier.size(60.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                        AppIconImage(drawable = app.icon, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(app.appName, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                            // fill empty slots
                            repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Websites Bottom Sheet ────────────────────────────────────
        if (showWebsSheet) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showWebsSheet = false })
            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color(0xFF111827), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allowed Sites", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showWebsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    session.allowedWebsites.forEach { site ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val url = if (site.startsWith("http")) site else "https://$site"
                                    if (chromeAllowed) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                                setPackage("com.android.chrome"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                        }
                                    } else {
                                        val intent = Intent(context, BpWebViewActivity::class.java).apply {
                                            putExtra("url", url)
                                            putExtra("allowed_sites", session.allowedWebsites.toTypedArray())
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                    showWebsSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = PremiumTealAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(site, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BpCountUnit(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center) {
            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun BpUnlockScreen(session: ButtonPhoneSession, onUnlocked: () -> Unit, onBack: () -> Unit) {
    var passInput by remember { mutableStateOf("") }
    var longInput by remember { mutableStateOf("") }
    var errMsg by remember { mutableStateOf("") }

    BackHandler(enabled = true) { onBack() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PremiumTealMid, PremiumTealDark))), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PremiumTealDark)
                    }
                    Text("Unlock Focus Mode", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PremiumTealDark)
                }
                Spacer(Modifier.height(24.dp))

                when (session.unlockMode) {
                    BpUnlockMode.PARENTS -> {
                        Text("Unlock password লিখুন:", fontSize = 14.sp, color = TextDark)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passInput, onValueChange = { passInput = it; errMsg = "" },
                            label = { Text("Password", color = TextGray) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PremiumTealMid, unfocusedBorderColor = GrayBg,
                                focusedTextColor = TextDark, unfocusedTextColor = TextDark)
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (passInput == session.parentPassword) onUnlocked()
                                else errMsg = "ভুল পাসওয়ার্ড!"
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumTealMid)
                        ) { Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
                    }

                    BpUnlockMode.SELF -> {
                        if (session.requireLongText) {
                            Text("নিচের পুরো লেখাটি হুবহু টাইপ করুন:", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().background(GrayBg, RoundedCornerShape(12.dp)).padding(14.dp)) {
                                Text(LONG_UNLOCK_TEXT, fontSize = 12.sp, color = TextDark.copy(alpha=0.8f), lineHeight = 18.sp,
                                    modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()))
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = longInput, onValueChange = { longInput = it; errMsg = "" },
                                placeholder = { Text("এখানে টাইপ করুন...", color = TextGray, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PremiumTealMid, unfocusedBorderColor = GrayBg,
                                    focusedTextColor = TextDark, unfocusedTextColor = TextDark)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (longInput.trim() == LONG_UNLOCK_TEXT.trim()) onUnlocked()
                                    else errMsg = "লেখা মিলছে না! হুবহু টাইপ করুন।"
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumTealMid)
                            ) { Text("Verify & Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
                        } else {
                            Text("Focus session শেষ করতে চান?", fontSize = 16.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onUnlocked,
                                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumTealMid)
                            ) { Text("Unlock Now", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
                        }
                    }
                }

                if (errMsg.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(errMsg, color = RedAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("← ফিরে যাও", color = TextGray, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun BpModeBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (selected) PremiumTealMid else Color.Transparent
    val tc = if (selected) Color.White else PremiumTealDark
    val bc = if (selected) PremiumTealMid else Color(0xFFDDE6FF)
    OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, bc),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg)) {
        Text(label, fontWeight = FontWeight.Bold, color = tc, fontSize = 14.sp)
    }
}