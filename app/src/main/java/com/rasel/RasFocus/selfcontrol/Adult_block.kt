package com.rasel.RasFocus.selfcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import com.rasel.RasFocus.DataManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class AdultBlockService : AccessibilityService() {

    companion object {
        var instance: AdultBlockService? = null
        private const val NOTIFICATION_CHANNEL_ID = "RasFocus_Channel"
        private const val NOTIFICATION_ID = 1001
    }

    // ── Adult Block: Keyword & Site Lists ──────────────────────────────────
    private val hardcoreKeywords = listOf(
        "porn", "xxx", "sex", "nude", "nsfw", "sexy", "hentai", "rule34", "milf",
        "blowjob", "tits", "boobs", "pussy", "dick", "cock", "escort", "bdsm",
        "fetish", "erotica", "dildo", "webcam", "camgirls", "xvideos", "pornhub",
        "xnxx", "xhamster", "brazzers", "onlyfans", "playboy", "chaturbate",
        "stripchat", "eporner", "spankbang", "redtube", "youporn", "mia khalifa",
        "sunny leone", "dani daniels", "johnny sins", "kendra lust",
        "চটি", "পর্ণ", "সেক্স", "নগ্ন", "উলঙ্গ", "বেশ্যা", "মাগি", "খানকি",
        "যৌন", "পর্ণগ্রাফি", "রেন্ডি", "চোদাচুতি", "গরম ভিডিও", "খারাপ ছবি",
        "যৌন মিলন", "যৌনাঙ্গ", "চুদো", "নগ্নতা"
    )

    private val romanticKeywords = listOf(
        "hot dance", "seductive dance", "item song", "belly dance", "hot",
        "kissing scene", "bikini", "swimsuit", "sexy dance", "cleavage", "hot scene",
        "romantic kiss", "bedroom scene", "bath scene", "rain dance", "bold scene",
        "semi nude", "lingerie", "erotic", "hot song", "romantic video hot",
        "navel show", "deep neck", "short dress sexy", "unfaithful scene"
    )

    private val adultWebsites = listOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "brazzers.com", "spankbang.com", "eporner.com", "chaturbate.com"
    )

    private var dynamicAdultList = listOf<String>()

    private val muslimQuotesBn = listOf("মুমিনদের বলুন, তারা যেন তাদের দৃষ্টি নত রাখে...", "লজ্জাশীলতা ঈমানের অঙ্গ।")
    private val muslimQuotesEn = listOf("Tell the believing men to reduce their vision...", "Modesty is a branch of faith.")
    private val hinduQuotesBn = listOf("যে মনকে নিয়ন্ত্রণ করতে পারে, তার মন তার সবচেয়ে বড় বন্ধু।", "কাম, ক্রোধ এবং লোভ—এই তিনটি নরকের দ্বার।")
    private val hinduQuotesEn = listOf("For him who has conquered the mind, the mind is the best of friends.", "Lust, anger, and greed are the three doors to hell.")
    private val christianQuotesBn = listOf("খারাপ সাহচর্য ভালো চরিত্র নষ্ট করে।", "অহংকার পতনের মূল।")
    private val christianQuotesEn = listOf("Bad company ruins good morals.", "Pride goes before destruction.")
    private val motivationalQuotesBn = listOf("সফলতা আসে ফোকাস থেকে, ডিস্ট্রাকশন থেকে নয়।", "যে নিজের মনকে নিয়ন্ত্রণ করতে পারে, সে পৃথিবী জয় করতে পারে।")
    private val motivationalQuotesEn = listOf("Success comes from focus, not from distraction.", "He who can control his mind can conquer the world.")

    // ── Logic States ───────────────────────────────────────────
    private var lastBlockTime: Long = 0
    private var lastLoggedUrl: String = ""
    
    // Panic Mode Logic
    private var isPanicActive = false
    private var panicEndTime = 0L

    // Periodic Popup Logic
    private val periodicHandler = Handler(Looper.getMainLooper())
    private var periodicRunnable: Runnable? = null

    // Deep Study Logic
    private var isDeepStudyActive = false
    private var isDeepStudyBreak = false
    private var windowManager: android.view.WindowManager? = null
    private var dsTimer: android.os.CountDownTimer? = null
    private var dsTimeLeftMillis: Long = 0
    
    // Overlays
    private var floatingTimerView: android.view.View? = null
    private var timerTextView: android.widget.TextView? = null
    private var breakScreenView: android.view.View? = null
    private var sessionCompleteView: android.view.View? = null
    private var fullScreenBlockView: android.view.View? = null
    private var isPopupVisible = false

    private var audioTrack: android.media.AudioTrack? = null
    private var isPlayingNoise = false
    private var noiseThread: Thread? = null

    private var initialX: Int = 0; private var initialY: Int = 0
    private var initialTouchX: Float = 0f; private var initialTouchY: Float = 0f
    private lateinit var recoveryPrefs: SharedPreferences
    private lateinit var adultPrefs: SharedPreferences // To fetch granular toggles

    override fun onCreate() {
        super.onCreate()
        DataManager.init(this)
        recoveryPrefs = getSharedPreferences("FocusRecovery", Context.MODE_PRIVATE)
        adultPrefs = getSharedPreferences("RasFocusAdultPrefs", Context.MODE_PRIVATE)
        createNotificationChannel()
        loadAdultSiteFile()
    }

    private fun loadAdultSiteFile() {
        try {
            val inputStream = assets.open("adultsite.txt")
            val text = inputStream.bufferedReader().use { it.readText() }
            dynamicAdultList = text.split("\n", "\r\n")
                .map { it.trim().lowercase().replace("*.", ".") }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            android.util.Log.e("RasFocus", "Error reading adultsite.txt: ${e.message}")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100 // Fast detection
        }
        this.serviceInfo = info

        startForeground(NOTIFICATION_ID, buildNotification("Protection is Active", "Monitoring your focus..."))
        startPeriodicPopupChecker()

        // Resume Deep Study if needed
        val isSavedActive = recoveryPrefs.getBoolean("isTimerActive", false)
        val targetEndTime = recoveryPrefs.getLong("targetEndTime", 0L)
        val sessionType = recoveryPrefs.getInt("sessionType", 0)
        val playSound = recoveryPrefs.getBoolean("playSound", false)
        val soundType = recoveryPrefs.getInt("soundType", 0)

        if (isSavedActive && targetEndTime > System.currentTimeMillis()) {
            val remainingMillis = targetEndTime - System.currentTimeMillis()
            if (sessionType == 0) resumeDeepStudySession(remainingMillis, playSound, soundType)
            else startDeepStudyBreak((remainingMillis / 60000).toInt())
        } else {
            recoveryPrefs.edit().clear().apply()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        stopPeriodicPopupChecker()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopAmbientSound()
        stopPeriodicPopupChecker()
    }

    // ── Periodic Popup (Reminders) ───────────────────────────────────────
    private fun startPeriodicPopupChecker() {
        periodicRunnable = object : Runnable {
            override fun run() {
                if (DataManager.isPeriodicPopupsActive && (DataManager.isAdultFocusActive || DataManager.is24HourLockActive)) {
                    showFullScreenBlockPopup("REMINDER", getMotivationalQuote(), "Stay Focused, Stay Pure.", "#0CA8B0")
                }
                periodicHandler.postDelayed(this, 25 * 60 * 1000L) // 25 mins
            }
        }
        periodicHandler.postDelayed(periodicRunnable!!, 25 * 60 * 1000L)
    }

    private fun stopPeriodicPopupChecker() {
        periodicRunnable?.let { periodicHandler.removeCallbacks(it) }
    }

    fun activatePanicMode() {
        isPanicActive = true
        panicEndTime = System.currentTimeMillis() + 15 * 60 * 1000L
        showFullScreenBlockPopup("PANIC MODE ACTIVATED", "All browsers will be blocked for 15 minutes.", "Emergency Lock", "#F2930C")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "RasFocus Protection", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, Class.forName("com.rasel.RasFocus.MainActivity")), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title).setContentText(content).setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent).setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun updateNotification(title: String, content: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun isSystemApp(packageName: String): Boolean {
        return packageName.contains("launcher") || packageName.contains("systemui") ||
                packageName.contains("dialer") || packageName.contains("messaging") ||
                packageName.contains("inputmethod") || packageName.contains("keyboard") ||
                packageName == "com.rasel.RasFocus"
    }

    private fun isBrowserApp(packageName: String): Boolean {
        return packageName.contains("chrome") || packageName.contains("browser") ||
               packageName.contains("edge") || packageName.contains("firefox") ||
               packageName.contains("brave") || packageName.contains("opera")
    }

    // ── Silent Monitor Logging ───────────────────────────────────────────
    private fun logSilentUrl(windowTitle: String, url: String) {
        if (url.isEmpty() || url == lastLoggedUrl) return
        lastLoggedUrl = url
        try {
            val timeStr = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(Date())
            val logData = "[$timeStr] TITLE: $windowTitle | URL: $url\n"
            val logFile = File(applicationContext.filesDir, "silent_monitor_log.txt")
            FileOutputStream(logFile, true).use { it.write(logData.toByteArray()) }
        } catch (e: Exception) {
            android.util.Log.e("RasFocus", "Silent Monitor Log Error: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Fetch Granular Toggles from SharedPreferences (Defaults to true to match C++ strictness)
        val cbAdultWeb = adultPrefs.getBoolean("cbAdultWeb", true)
        val cbHardcore = adultPrefs.getBoolean("cbHardcore", true)
        val cbRomantic = adultPrefs.getBoolean("cbRomantic", true)
        val cbFbReels = adultPrefs.getBoolean("cbFbReels", true)
        val cbYtShorts = adultPrefs.getBoolean("cbYtShorts", true)

        // ── 1. PANIC MODE CHECK (Priority #1) ────────────────────────────
        if (isPanicActive) {
            if (System.currentTimeMillis() < panicEndTime) {
                if (isBrowserApp(packageName)) {
                    multiLayerForceHome()
                    if (System.currentTimeMillis() - lastBlockTime > 5000) {
                        lastBlockTime = System.currentTimeMillis()
                        showFullScreenBlockPopup("PANIC MODE", "All browsers are disabled for 15 mins.", "Emergency Protocol Active", "#E74C3C")
                    }
                    return
                }
            } else {
                isPanicActive = false
            }
        }

        if (!DataManager.isFocusActive && !DataManager.isAdultFocusActive && !isDeepStudyActive && !DataManager.is24HourLockActive) return

        // ── 2. KEYBOARD REALTIME TYPING BLOCK (With Granular Checks) ──────
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val isKeyboardBlockEnabled = adultPrefs.getBoolean("keyboardTypingBlock", false)

            if (!isSystemApp(packageName) && isKeyboardBlockEnabled) {
                val source = event.source ?: return
                val typedText = source.text?.toString()?.lowercase() ?: event.text.joinToString(" ").lowercase()
                if (typedText.isBlank()) return

                val customKeywords = DataManager.userCustomAdultKeywords.map { it.lowercase() }
                
                // Only match if the respective setting is enabled
                val isHardcoreMatch = cbHardcore && hardcoreKeywords.any { typedText.contains(it) }
                val isRomanticMatch = cbRomantic && romanticKeywords.any { typedText.contains(it) }
                val isDynamicMatch  = cbAdultWeb && dynamicAdultList.any { site ->
                    val domain = site.replace(".", "").replace("www", "")
                    domain.length > 3 && typedText.contains(domain)
                }
                val isCustomMatch = customKeywords.isNotEmpty() && customKeywords.any { typedText.contains(it) }

                if (isHardcoreMatch || isRomanticMatch || isDynamicMatch || isCustomMatch) {
                    try {
                        val clearArgs = android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "") }
                        if (!source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)) {
                            val selectArgs = android.os.Bundle().apply {
                                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, typedText.length)
                            }
                            source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
                            source.performAction(AccessibilityNodeInfo.ACTION_CUT)
                        }
                    } catch (e: Exception) {}

                    if (DataManager.isAdultFocusActive || DataManager.is24HourLockActive) {
                        triggerAdultBlockAction(packageName, "Blocked Keyword Typed")
                    }
                    return
                }
            }
        }

        // ── 3. 24H LOCK SYNC ─────────────────────────────────────────────
        if (DataManager.is24HourLockActive) {
            if (System.currentTimeMillis() >= DataManager.lock24hEndTime) {
                DataManager.is24HourLockActive = false
                DataManager.isAdultFocusActive = false
            } else {
                DataManager.isAdultFocusActive = true
            }
        }

        // ── 4. URL & WINDOW DETECTION ────────────────────────────────────
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val rootNode = rootInActiveWindow ?: return
            var currentUrl = ""
            var windowTitle = ""

            if (isBrowserApp(packageName)) {
                currentUrl = extractUrlFromBrowser(rootNode).lowercase()
                windowTitle = extractWindowTitle(rootNode)
            }

            val screenText = event.text.joinToString(" ").lowercase()

            // ── Strict Protocols: Block Incognito ──
            val blockIncognito = adultPrefs.getBoolean("strictBlockIncognito", false)
            if (blockIncognito && isBrowserApp(packageName)) {
                if (screenText.contains("incognito") || screenText.contains("inprivate") || screenText.contains("private browsing") || windowTitle.lowercase().contains("incognito")) {
                    multiLayerForceHome()
                    showFullScreenBlockPopup("STRICT MODE", "Incognito / Private windows are not allowed.", "Strict Protocol Active", "#0CA8B0")
                    rootNode.recycle()
                    return
                }
            }

            // ── Strict Protocols: Settings & Uninstaller Block ──
            val strictLockMode = adultPrefs.getBoolean("strictLockMode", false)
            if (strictLockMode || DataManager.blockSettingsAndUninstall) {
                if (packageName.contains("com.android.settings") || packageName.contains("packageinstaller") || packageName.contains("taskmanager")) {
                    multiLayerForceHome()
                    showFullScreenBlockPopup("ACCESS DENIED", "System Settings / Uninstallers are locked.", "Strict Lock Mode", "#E74C3C")
                    rootNode.recycle()
                    return
                }
            }

            if (isDeepStudyActive && DataManager.isDeepStudyStrict) {
                checkDeepStudyBlocking(packageName, currentUrl)
            } else {
                checkAndBlockContent(packageName, currentUrl, screenText, windowTitle, cbAdultWeb, cbHardcore, cbRomantic, cbFbReels, cbYtShorts)
            }
            rootNode.recycle()
        }
    }

    private fun extractUrlFromBrowser(nodeInfo: AccessibilityNodeInfo?): String {
        if (nodeInfo == null) return ""
        if (nodeInfo.className == "android.widget.EditText") {
            val id = nodeInfo.viewIdResourceName
            if (id != null && (id.contains("url_bar") || id.contains("address_bar") || id.contains("search_box_text"))) {
                return nodeInfo.text?.toString() ?: ""
            }
        }
        for (i in 0 until nodeInfo.childCount) {
            val url = extractUrlFromBrowser(nodeInfo.getChild(i))
            if (url.isNotEmpty()) return url
        }
        return ""
    }

    private fun extractWindowTitle(nodeInfo: AccessibilityNodeInfo?): String {
        if (nodeInfo == null) return ""
        if (nodeInfo.className == "android.widget.TextView") {
            val id = nodeInfo.viewIdResourceName
            if (id != null && id.contains("title")) return nodeInfo.text?.toString() ?: ""
        }
        for (i in 0 until nodeInfo.childCount) {
            val title = extractWindowTitle(nodeInfo.getChild(i))
            if (title.isNotEmpty()) return title
        }
        return ""
    }

    // ── MULTI-LAYER FORCE HOME LOGIC (Bulletproof Exit) ─────────────────
    private fun multiLayerForceHome() {
        // Layer 1: Soft Exit via Back Button
        performGlobalAction(GLOBAL_ACTION_BACK)
        Thread.sleep(100)
        
        // Layer 2: Standard Home Action
        val homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME)
        
        // Layer 3: Hard Intent Fallback if System ignored Accessibility Home
        if (!homeSuccess) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            try {
                startActivity(homeIntent)
            } catch (e: Exception) {
                android.util.Log.e("RasFocus", "Failed Layer 3 Home Intent: ${e.message}")
            }
        }
    }

    private fun checkDeepStudyBlocking(packageName: String, url: String) {
        if (isSystemApp(packageName)) return
        val allowedApps = DataManager.dsAllowAppList
        val allowedWebs = DataManager.dsAllowWebList
        val isAppAllowed = allowedApps.any { packageName.contains(it, ignoreCase = true) }
        val isWebAllowed = url.isNotEmpty() && allowedWebs.any { url.contains(it.substringBefore("."), ignoreCase = true) }
        val pauseDuringBreak = isDeepStudyBreak && !DataManager.dsKeepBlockingInBreak

        if (!isAppAllowed && !isWebAllowed && !pauseDuringBreak) {
            if (System.currentTimeMillis() - lastBlockTime < 5000) return
            lastBlockTime = System.currentTimeMillis()
            multiLayerForceHome()
            showFullScreenBlockPopup("STAY FOCUSED!", getMotivationalQuote(), "Reason: App/Website is restricted during Deep Study.", "#4A00E0")
        }
    }

    private fun checkAndBlockContent(packageName: String, url: String, screenText: String, windowTitle: String, 
                                     cbAdultWeb: Boolean, cbHardcore: Boolean, cbRomantic: Boolean, cbFbReels: Boolean, cbYtShorts: Boolean) {
        var shouldBlockNormal = false
        var isAdultViolation = false
        var blockReason = ""

        if (DataManager.isAdultFocusActive || DataManager.is24HourLockActive) {
            when {
                cbAdultWeb && dynamicAdultList.any { url.contains(it) || screenText.contains(it) } -> {
                    isAdultViolation = true; blockReason = "Restricted Website / Keyword"
                }
                cbAdultWeb && adultWebsites.any { url.contains(it) || screenText.contains(it.substringBefore(".")) } -> {
                    isAdultViolation = true; blockReason = "Adult Website Detected"
                }
                cbHardcore && hardcoreKeywords.any { url.contains(it) || screenText.contains(it) } -> {
                    isAdultViolation = true; blockReason = "Explicit Keyword Detected"
                }
                cbRomantic && romanticKeywords.any { url.contains(it) || screenText.contains(it) } -> {
                    isAdultViolation = true; blockReason = "Softcore/Romantic Content Detected"
                }
                cbYtShorts && ((packageName.contains("youtube") && screenText.contains("shorts")) || url.contains("shorts")) -> {
                    shouldBlockNormal = true; blockReason = "YouTube Shorts are blocked!"
                }
                cbFbReels && ((packageName.contains("facebook") && screenText.contains("reels")) || url.contains("reel") || url.contains("instagram.com/reels")) -> {
                    shouldBlockNormal = true; blockReason = "FB / IG Reels are blocked!"
                }
            }

            // ── Silent Monitor (Only log if no violation occurred yet) ──
            val isSilentMonitorActive = adultPrefs.getBoolean("strictSilentMonitor", false)
            if (!isAdultViolation && isSilentMonitorActive && isBrowserApp(packageName) && url.isNotEmpty()) {
                logSilentUrl(windowTitle, url)
            }
        }

        if (DataManager.isFocusActive && !shouldBlockNormal && !isAdultViolation && url.isNotEmpty()) {
            for (web in DataManager.userWebList) {
                val coreName = if (web.contains(".")) web.substringBefore(".") else web
                if (coreName.length > 2 && url.contains(coreName)) {
                    shouldBlockNormal = true; blockReason = "Website is in your blocklist."; break
                }
            }
        }

        if (!shouldBlockNormal && !isAdultViolation && DataManager.isFocusActive) {
            if (DataManager.simpleBlockMode == 1) {
                if (DataManager.userAppList.any { packageName.contains(it) }) {
                    shouldBlockNormal = true; blockReason = "App is in your blocklist."
                }
            } else if (DataManager.simpleBlockMode == 0) {
                if (!isSystemApp(packageName) && !DataManager.userAppList.any { packageName.contains(it) }) {
                    shouldBlockNormal = true; blockReason = "Only allowed apps can run."
                }
            }
        }

        if (isAdultViolation) {
            triggerAdultBlockAction(packageName, blockReason)
        } else if (shouldBlockNormal) {
            if (System.currentTimeMillis() - lastBlockTime < 5000) return
            lastBlockTime = System.currentTimeMillis()
            multiLayerForceHome()
            val mainMsg = if (DataManager.showQuotes) getReligiousQuote() else getMotivationalQuote()
            showFullScreenBlockPopup("ACCESS DENIED!", mainMsg, "Reason: $blockReason", "#0CA8B0")
        }
    }

    private fun triggerAdultBlockAction(packageName: String, reason: String) {
        if (System.currentTimeMillis() - lastBlockTime < 5000) return
        lastBlockTime = System.currentTimeMillis()

        multiLayerForceHome()

        DataManager.totalBlockedCount++
        DataManager.cleanStreakDays = 0

        showFullScreenBlockPopup("ASTAGFIRULLAH!", getReligiousQuote(), "Reason: $reason", "#F12B2C")
    }

    private fun getReligiousQuote(): String {
        val list = when (DataManager.adultReligion) {
            0 -> if (DataManager.adultLanguage == 0) muslimQuotesBn else muslimQuotesEn
            1 -> if (DataManager.adultLanguage == 0) hinduQuotesBn else hinduQuotesEn
            2 -> if (DataManager.adultLanguage == 0) christianQuotesBn else christianQuotesEn
            else -> if (DataManager.adultLanguage == 0) motivationalQuotesBn else motivationalQuotesEn
        }
        return list[Random.nextInt(list.size)]
    }

    private fun getMotivationalQuote(): String {
        val list = if (DataManager.adultLanguage == 0) motivationalQuotesBn else motivationalQuotesEn
        return list[Random.nextInt(list.size)]
    }

    // ── Public: Stop Focus API (Password / Long Text) ────────────────────
    fun tryStopFocus(input: String): Boolean {
        if (DataManager.is24HourLockActive) return false
        
        // Mode 1: Friend Password
        if (DataManager.controlMode == 1) {
            val prefs = getSharedPreferences("RasFocusData", Context.MODE_PRIVATE)
            val savedPassword = prefs.getString("friendPassword", "1234") ?: "1234"
            if (input == savedPassword) {
                DataManager.isAdultFocusActive = false
                return true
            }
            return false
        }
        
        // Mode 2: Long Text
        if (DataManager.controlMode == 2) {
            val savedLongText = adultPrefs.getString("savedLongText", "") ?: ""
            if (input.isNotEmpty() && input.trim() == savedLongText.trim()) {
                DataManager.isAdultFocusActive = false
                return true
            }
            return false
        }
        
        // Mode 0: Self
        DataManager.isAdultFocusActive = false
        return true
    }

    // ── Deep Study Session ────────────────────────────────────────────────
    fun startDeepStudySession(focusMinutes: Int, playSound: Boolean, soundType: Int = 0) {
        resumeDeepStudySession(focusMinutes * 60 * 1000L, playSound, soundType)
    }

    private fun resumeDeepStudySession(timeMillis: Long, playSound: Boolean, soundType: Int) {
        isDeepStudyActive = true; isDeepStudyBreak = false
        recoveryPrefs.edit()
            .putBoolean("isTimerActive", true)
            .putLong("targetEndTime", System.currentTimeMillis() + timeMillis)
            .putInt("sessionType", 0).putBoolean("playSound", playSound).putInt("soundType", soundType).apply()
        if (playSound) playAmbientSound(soundType)
        showFloatingTimer()
        dsTimer?.cancel()
        dsTimer = object : android.os.CountDownTimer(timeMillis, 30) {
            override fun onTick(ms: Long) {
                dsTimeLeftMillis = ms
                updateFloatingTimerText(ms)
                if (ms in 59000..60030) showFullScreenBlockPopup("KEEP GOING!", "⏳ Just 1 Minute Remaining!", "Reason: Deep Study Session Alert", "#4A00E0")
            }
            override fun onFinish() {
                stopAmbientSound(); removeFloatingTimer()
                isDeepStudyActive = false; DataManager.isDeepStudyStrict = false
                recoveryPrefs.edit().clear().apply()
                sendBroadcast(Intent("POMODORO_SESSION_UPDATE"))
                updateNotification("Protection is Active", "Monitoring your focus...")
                showSessionCompletePopup()
            }
        }.start()
    }

    private fun startDeepStudyBreak(breakMinutes: Int) {
        isDeepStudyBreak = true
        val timeMillis = breakMinutes * 60 * 1000L
        showBreakScreenOverlay()
        recoveryPrefs.edit().putBoolean("isTimerActive", true)
            .putLong("targetEndTime", System.currentTimeMillis() + timeMillis).putInt("sessionType", 1).apply()
        dsTimer?.cancel()
        dsTimer = object : android.os.CountDownTimer(timeMillis, 1000) {
            override fun onTick(ms: Long) { updateNotification("Break Time!", "Enjoy your break. ${ms / 60000} mins left.") }
            override fun onFinish() {
                removeBreakScreenOverlay(); isDeepStudyActive = false; DataManager.isDeepStudyStrict = false
                recoveryPrefs.edit().clear().apply()
                updateNotification("Protection is Active", "Monitoring your focus...")
                showFullScreenBlockPopup("TIME'S UP!", "🎉 Break Completed! Ready to focus?", "Reason: Deep Study Break Ended", "#0CA8B0")
                sendBroadcast(Intent("POMODORO_SESSION_UPDATE"))
            }
        }.start()
    }

    private fun showSessionCompletePopup() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (sessionCompleteView != null) return@post
            windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val params = android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#E6000000")); isClickable = true; isFocusable = true
            }
            val card = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
                setPadding(60, 80, 60, 80)
                val shape = android.graphics.drawable.GradientDrawable(); shape.cornerRadius = 40f; shape.setColor(android.graphics.Color.WHITE); background = shape
                layoutParams = android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(80, 0, 80, 0) }
            }
            val title = android.widget.TextView(this).apply { text = "SESSION COMPLETED! 🎉"; textSize = 22f; setTextColor(android.graphics.Color.parseColor("#0CA8B0")); setTypeface(null, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER; setPadding(0, 0, 0, 60) }
            val btnRest = makeBtn("Take a Rest (${DataManager.dsRestMin}m)", "#10B981") { removeSessionCompletePopup(); startDeepStudyBreak(DataManager.dsRestMin) }
            val btnStart = makeBtn("Start Again (${DataManager.dsFocusMin}m)", "#0CA8B0") { removeSessionCompletePopup(); startDeepStudySession(DataManager.dsFocusMin, recoveryPrefs.getBoolean("playSound", false), recoveryPrefs.getInt("soundType", 0)) }
            val btnClose = makeBtn("Close & Reset", "#E74C3C") { removeSessionCompletePopup() }
            card.addView(title); card.addView(btnRest); card.addView(btnStart); card.addView(btnClose)
            layout.addView(card); sessionCompleteView = layout
            try { windowManager?.addView(sessionCompleteView, params) } catch (e: Exception) {}
        }
    }

    private fun makeBtn(label: String, colorHex: String, onClick: () -> Unit): android.widget.Button {
        return android.widget.Button(this).apply {
            text = label; setTextColor(android.graphics.Color.WHITE)
            val s = android.graphics.drawable.GradientDrawable(); s.cornerRadius = 24f; s.setColor(android.graphics.Color.parseColor(colorHex)); background = s
            layoutParams = android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 140).apply { setMargins(0, 0, 0, 30) }
            setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_UP) onClick(); true }
        }
    }

    private fun removeSessionCompletePopup() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            sessionCompleteView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }; sessionCompleteView = null
        }
    }

    private fun playAmbientSound(soundType: Int) {
        if (isPlayingNoise) return; isPlayingNoise = true
        val sampleRate = 44100
        val bufferSize = android.media.AudioTrack.getMinBufferSize(sampleRate, android.media.AudioFormat.CHANNEL_OUT_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = android.media.AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate, android.media.AudioFormat.CHANNEL_OUT_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT, bufferSize, android.media.AudioTrack.MODE_STREAM)
        audioTrack?.play()
        noiseThread = Thread {
            val buffer = ShortArray(bufferSize); val random = java.util.Random(); var lastOut = 0.0; var phase = 0.0
            while (isPlayingNoise) {
                for (i in buffer.indices) {
                    val white = (random.nextDouble() * 2 - 1); var output = 0.0
                    when (soundType) {
                        0 -> output = white * 0.1
                        1 -> { lastOut = (lastOut + 0.02 * white) / 1.02; output = lastOut * 3.5 }
                        2 -> { lastOut = (lastOut + 0.01 * white) / 1.01; output = lastOut * 4.5 }
                        3 -> { lastOut = (lastOut + 0.04 * white) / 1.04; output = lastOut * 2.5 }
                        4 -> { lastOut = (lastOut + 0.02 * white) / 1.02; output = lastOut * 3.5 + (if (random.nextDouble() > 0.99) white * 0.3 else 0.0) }
                        5 -> { lastOut = (lastOut + 0.02 * white) / 1.02; output = lastOut * 2.0 + white * 0.05 }
                        6 -> { lastOut = (lastOut + 0.015 * white) / 1.015; phase += 0.0001; val mod = Math.sin(phase) * 0.5 + 0.5; output = lastOut * 3.0 * (0.4 + 0.6 * mod) }
                        7 -> { lastOut = (lastOut + 0.005 * white) / 1.005; output = lastOut * 6.0 }
                        8 -> { lastOut = (lastOut + 0.008 * white) / 1.008; phase += 0.0005; val drone = Math.sin(phase) * 0.15; output = lastOut * 4.0 + drone }
                        9 -> { lastOut = (lastOut + 0.01 * white) / 1.01; phase += 0.0002; val throb = Math.sin(phase) * 0.3; output = lastOut * 3.5 * (0.7 + throb) }
                        else -> { lastOut = (lastOut + 0.02 * white) / 1.02; output = lastOut * 3.5 }
                    }
                    buffer[i] = (output.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }; noiseThread?.start()
    }

    private fun stopAmbientSound() {
        isPlayingNoise = false
        try { noiseThread?.join(500) } catch (e: Exception) {}
        audioTrack?.let { if (it.playState == android.media.AudioTrack.PLAYSTATE_PLAYING) it.stop(); it.release() }; audioTrack = null
    }

    private fun showFloatingTimer() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (floatingTimerView != null) return@post
            windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val params = android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.WRAP_CONTENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.START; x = 100; y = 200 }
            val layout = android.widget.LinearLayout(this).apply {
                setPadding(40, 20, 40, 20)
                val shape = android.graphics.drawable.GradientDrawable(); shape.cornerRadius = 30f; shape.setColor(android.graphics.Color.parseColor("#0CA8B0")); background = shape
                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; true }
                        android.view.MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - initialTouchX).toInt(); params.y = initialY + (event.rawY - initialTouchY).toInt(); windowManager?.updateViewLayout(this, params); true }
                        else -> false
                    }
                }
            }
            timerTextView = android.widget.TextView(this).apply { setTextColor(android.graphics.Color.WHITE); textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD); text = "00:00:00" }
            layout.addView(timerTextView); floatingTimerView = layout
            try { windowManager?.addView(floatingTimerView, params) } catch (e: Exception) {}
        }
    }

    private fun updateFloatingTimerText(millis: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val mins = (millis / 1000) / 60; val secs = (millis / 1000) % 60; val ms = (millis % 1000) / 10
            timerTextView?.text = String.format("%02d:%02d:%02d", mins, secs, ms)
            updateNotification("Deep Study Active", "Time remaining: ${String.format("%02d:%02d", mins, secs)}")
        }
    }

    private fun removeFloatingTimer() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            floatingTimerView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }; floatingTimerView = null
        }
    }

    private fun showBreakScreenOverlay() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (breakScreenView != null) return@post
            windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val params = android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, intArrayOf(android.graphics.Color.parseColor("#4A00E0"), android.graphics.Color.parseColor("#8E2DE2")))
            }
            val t = android.widget.TextView(this).apply { text = "TAKE A BREAK!"; textSize = 45f; setTextColor(android.graphics.Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 30); gravity = android.view.Gravity.CENTER }
            val s = android.widget.TextView(this).apply { text = "Breathe deep, rest your eyes, and relax your mind."; textSize = 18f; setTextColor(android.graphics.Color.parseColor("#E2E8F0")); gravity = android.view.Gravity.CENTER }
            layout.addView(t); layout.addView(s); breakScreenView = layout
            try { windowManager?.addView(breakScreenView, params) } catch (e: Exception) {}
        }
    }

    private fun removeBreakScreenOverlay() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            breakScreenView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }; breakScreenView = null
        }
    }

    private fun showFullScreenBlockPopup(title: String, message: String, reasonInfo: String, bgColorHex: String) {
        if (isPopupVisible) return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (isPopupVisible) return@post
            isPopupVisible = true

            val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            windowManager = wm
            val dp = resources.displayMetrics.density
            val ctx = this

            val isAdult   = bgColorHex == "#F12B2C"
            val isStudy   = bgColorHex == "#4A00E0"
            val accentHex = when { isAdult -> "#F12B2C"; isStudy -> "#7C3AED"; else -> "#0CA8B0" }
            val iconEmoji = when { isAdult -> "⚠️"; isStudy -> "📚"; else -> "🛡" }

            val root = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                background = null; setPadding(0, 0, 0, 0)
            }

            val card = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#EE0D1117"))
                    cornerRadius = 20f * dp
                    setStroke((1.5f * dp).toInt(), android.graphics.Color.parseColor("#2A3040"))
                }
                background = bg
                val pad = (16 * dp).toInt(); setPadding(pad, pad, pad, pad)
            }

            val topRow = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val badge = android.widget.TextView(ctx).apply { text = iconEmoji; textSize = 18f; setPadding(0, 0, (8 * dp).toInt(), 0) }
            val appLabel = android.widget.TextView(ctx).apply {
                text = "RasFocus+"; textSize = 11f; setTextColor(android.graphics.Color.parseColor(accentHex))
                typeface = android.graphics.Typeface.DEFAULT_BOLD; letterSpacing = 0.08f
            }
            val spacer = android.widget.Space(ctx).apply { layoutParams = android.widget.LinearLayout.LayoutParams(0, 1, 1f) }
            val dismissBtn = android.widget.TextView(ctx).apply {
                text = "✕"; textSize = 13f; setTextColor(android.graphics.Color.parseColor("#6B7280"))
                setOnClickListener { removeFullScreenBlockPopup() }
                val p = (6 * dp).toInt(); setPadding(p, p, p, p)
            }
            topRow.addView(badge); topRow.addView(appLabel); topRow.addView(spacer); topRow.addView(dismissBtn)

            val divider = android.view.View(ctx).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#1E222C"))
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1f * dp).toInt()).apply { setMargins(0, (10 * dp).toInt(), 0, (10 * dp).toInt()) }
            }

            val titleView = android.widget.TextView(ctx).apply {
                text = "⊘  $title"; textSize = 15f; setTextColor(android.graphics.Color.parseColor("#EAEDF3")); typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val reasonView = android.widget.TextView(ctx).apply {
                text = reasonInfo; textSize = 12f; setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                setPadding(0, (5 * dp).toInt(), 0, 0); setLineSpacing(0f, 1.3f)
            }

            val quoteDivider = android.view.View(ctx).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#1A2436"))
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1f * dp).toInt()).apply { setMargins(0, (12 * dp).toInt(), 0, (10 * dp).toInt()) }
            }
            val quoteRow = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
            val accentBar = android.view.View(ctx).apply {
                setBackgroundColor(android.graphics.Color.parseColor(accentHex))
                layoutParams = android.widget.LinearLayout.LayoutParams((3f * dp).toInt(), android.widget.LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0, 0, (10 * dp).toInt(), 0) }
            }
            val quoteCol = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val quoteView = android.widget.TextView(ctx).apply {
                text = "\u201C$message\u201D"; textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#CBD5E0"))
                setTypeface(typeface, android.graphics.Typeface.ITALIC); setLineSpacing(0f, 1.35f)
            }
            val authorView = android.widget.TextView(ctx).apply {
                text = "— RasFocus"; textSize = 10f; setTextColor(android.graphics.Color.parseColor(accentHex))
                typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            quoteCol.addView(quoteView); quoteCol.addView(authorView); quoteRow.addView(accentBar); quoteRow.addView(quoteCol)

            val tagRow = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0, (12 * dp).toInt(), 0, 0)
            }
            val tagBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#0F1A13")); cornerRadius = 20f * dp; setStroke((1f * dp).toInt(), android.graphics.Color.parseColor("#00F5C430"))
            }
            val tagText = android.widget.TextView(ctx).apply {
                text = "● Active — Blocked by RasFocus+"; textSize = 10f; setTextColor(android.graphics.Color.parseColor("#00F5C4"))
                letterSpacing = 0.05f; background = tagBg
                val hp = (10 * dp).toInt(); val vp = (4 * dp).toInt(); setPadding(hp, vp, hp, vp)
            }
            tagRow.addView(tagText)

            card.addView(topRow); card.addView(divider); card.addView(titleView); card.addView(reasonView)
            card.addView(quoteDivider); card.addView(quoteRow); card.addView(tagRow); root.addView(card)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else @Suppress("DEPRECATION") android.view.WindowManager.LayoutParams.TYPE_PHONE
            val params = android.view.WindowManager.LayoutParams(
                (300 * dp).toInt(), android.view.WindowManager.LayoutParams.WRAP_CONTENT, type,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL; y = (60 * dp).toInt() }

            fullScreenBlockView = root
            try {
                wm.addView(fullScreenBlockView, params)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ removeFullScreenBlockPopup() }, 3000)
            } catch (e: Exception) { isPopupVisible = false }
        }
    }

    private fun removeFullScreenBlockPopup() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            fullScreenBlockView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }
            fullScreenBlockView = null; isPopupVisible = false
        }
    }

    override fun onInterrupt() {}
}

// ── Adult Block Screen Composable ──────────────────────────────────────────

@Composable
fun Adult_block() {
    AdultBlockScreen()
}