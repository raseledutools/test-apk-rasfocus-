// ============================================================
// RasFocusSettingsComplete.kt
// ONE FILE — Full Settings UI (Jetpack Compose) + All Blocking Logic
//
// SECTIONS:
//  A. Data / Prefs
//  B. Blocking Service (Accessibility)
//  C. All Blocking Handlers (inline)
//  D. Settings UI (Compose)
// ============================================================

package com.rasel.RasFocus.selfcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.concurrent.TimeUnit


// ════════════════════════════════════════════════════════════
// A. PREFS — সব toggle এর state এখানে
// ════════════════════════════════════════════════════════════

class BlockerPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("blocker_prefs", Context.MODE_PRIVATE)

    // ── Content Blocking ──
    var blockAdult: Boolean
        get() = prefs.getBoolean("adult", true)
        set(v) = prefs.edit().putBoolean("adult", v).apply()

    // adultsite.txt ফাইল থেকে লোড করা domain list দিয়ে ব্লক
    var blockAdultSiteList: Boolean
        get() = prefs.getBoolean("adult_site_list", false)
        set(v) = prefs.edit().putBoolean("adult_site_list", v).apply()

    var blockSearch: Boolean
        get() = prefs.getBoolean("search", false)
        set(v) = prefs.edit().putBoolean("search", v).apply()

    var blockReels: Boolean
        get() = prefs.getBoolean("reels", false)
        set(v) = prefs.edit().putBoolean("reels", v).apply()

    var blockInstaSearch: Boolean
        get() = prefs.getBoolean("insta_search", false)
        set(v) = prefs.edit().putBoolean("insta_search", v).apply()

    var blockYtShorts: Boolean
        get() = prefs.getBoolean("yt_shorts", true)
        set(v) = prefs.edit().putBoolean("yt_shorts", v).apply()

    var blockWaChannels: Boolean
        get() = prefs.getBoolean("wa_channels", false)
        set(v) = prefs.edit().putBoolean("wa_channels", v).apply()

    // ── নতুন: Block Reels/Shorts per-app prefs ──
    var blockInstaStories: Boolean
        get() = prefs.getBoolean("insta_stories", false)
        set(v) = prefs.edit().putBoolean("insta_stories", v).apply()

    var blockWaStatus: Boolean
        get() = prefs.getBoolean("wa_status", false)
        set(v) = prefs.edit().putBoolean("wa_status", v).apply()

    var blockWaBusinessStatus: Boolean
        get() = prefs.getBoolean("wa_biz_status", false)
        set(v) = prefs.edit().putBoolean("wa_biz_status", v).apply()

    var blockWaBusinessChannels: Boolean
        get() = prefs.getBoolean("wa_biz_channels", false)
        set(v) = prefs.edit().putBoolean("wa_biz_channels", v).apply()

    var blockSnapSpotlight: Boolean
        get() = prefs.getBoolean("snap_spotlight", false)
        set(v) = prefs.edit().putBoolean("snap_spotlight", v).apply()

    var blockSnapStories: Boolean
        get() = prefs.getBoolean("snap_stories", false)
        set(v) = prefs.edit().putBoolean("snap_stories", v).apply()

    var blockTikTok: Boolean
        get() = prefs.getBoolean("tiktok", false)
        set(v) = prefs.edit().putBoolean("tiktok", v).apply()

    var blockTikTokLive: Boolean
        get() = prefs.getBoolean("tiktok_live", false)
        set(v) = prefs.edit().putBoolean("tiktok_live", v).apply()

    // ── Advanced Blocking ──
    var blockUnsupported: Boolean
        get() = prefs.getBoolean("unsupported_browser", false)
        set(v) = prefs.edit().putBoolean("unsupported_browser", v).apply()

    var blockNewApps: Boolean
        get() = prefs.getBoolean("new_apps", false)
        set(v) = prefs.edit().putBoolean("new_apps", v).apply()

    var blockFbVideo: Boolean
        get() = prefs.getBoolean("fb_video", false)
        set(v) = prefs.edit().putBoolean("fb_video", v).apply()

    // ── Uninstall Protection ──
    var uninstallProtection: Boolean
        get() = prefs.getBoolean("uninstall_prot", false)
        set(v) = prefs.edit().putBoolean("uninstall_prot", v).apply()

    // ── Reboot / Power Protection (নতুন switches) ──
    var blockAdb: Boolean
        get() = prefs.getBoolean("block_adb", false)
        set(v) = prefs.edit().putBoolean("block_adb", v).apply()

    var blockPowerOff: Boolean
        get() = prefs.getBoolean("block_poweroff", false)
        set(v) = prefs.edit().putBoolean("block_poweroff", v).apply()

    var blockSafeMode: Boolean
        get() = prefs.getBoolean("block_safemode", false)
        set(v) = prefs.edit().putBoolean("block_safemode", v).apply()

    var blockReboot: Boolean
        get() = prefs.getBoolean("block_reboot", false)
        set(v) = prefs.edit().putBoolean("block_reboot", v).apply()

    var blockRecovery: Boolean
        get() = prefs.getBoolean("block_recovery", false)
        set(v) = prefs.edit().putBoolean("block_recovery", v).apply()

    // ── Customize Blocked Screen ──
    var blockedMessage: String
        get() = prefs.getString("blocked_msg", "This page is blocked.") ?: "This page is blocked."
        set(v) = prefs.edit().putString("blocked_msg", v).apply()

    var blockedCountdown: Int
        get() = prefs.getInt("blocked_countdown", 3)
        set(v) = prefs.edit().putInt("blocked_countdown", v).apply()

    var redirectUrl: String
        get() = prefs.getString("redirect_url", "https://www.google.com") ?: "https://www.google.com"
        set(v) = prefs.edit().putString("redirect_url", v).apply()

    // ── Focus Lock ──
    // mode: "none" | "self" | "parents" | "longtext"
    var focusLockMode: String
        get() = prefs.getString("focus_lock_mode", "none") ?: "none"
        set(v) = prefs.edit().putString("focus_lock_mode", v).apply()

    var focusLockActive: Boolean
        get() = prefs.getBoolean("focus_lock_active", false)
        set(v) = prefs.edit().putBoolean("focus_lock_active", v).apply()

    // Self mode: end time in millis (System.currentTimeMillis + duration)
    var focusLockEndTime: Long
        get() = prefs.getLong("focus_lock_end_time", 0L)
        set(v) = prefs.edit().putLong("focus_lock_end_time", v).apply()

    // Parents mode: hashed password (simple SHA-like, store as string)
    var focusLockPassword: String
        get() = prefs.getString("focus_lock_password", "") ?: ""
        set(v) = prefs.edit().putString("focus_lock_password", v).apply()

    // Long text mode: the required text (100-word passage)
    var focusLockLongText: String
        get() = prefs.getString("focus_lock_long_text", "") ?: ""
        set(v) = prefs.edit().putString("focus_lock_long_text", v).apply()

    // UI language: "bn" | "en"
    var focusLockLang: String
        get() = prefs.getString("focus_lock_lang", "bn") ?: "bn"
        set(v) = prefs.edit().putString("focus_lock_lang", v).apply()
}


// ════════════════════════════════════════════════════════════
// B. ACCESSIBILITY SERVICE — Main Entry Point
// ════════════════════════════════════════════════════════════

class RasFocusBlockingService : AccessibilityService() {

    companion object {
        var instance: RasFocusBlockingService? = null
    }

    private lateinit var prefs: BlockerPrefs
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager

    // ════════════════════════════════════════════════════════
    // OVERLAY — Block কারণ সহ popup দেখায়, তারপর HOME
    // ════════════════════════════════════════════════════════

    // ── Motivational Quotes — feature অনুযায়ী আলাদা list ──
    // প্রতিটা entry: Pair(quote, author)
    private val QUOTES_DEFAULT = listOf(
        Pair("Your time is limited, do not waste it living someone else's life.", "Steve Jobs"),
        Pair("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        Pair("He who controls others may be powerful, but he who masters himself is mightier still.", "Epictetus"),
        Pair("Attention is the rarest and purest form of generosity — spend it wisely.", "Cal Newport"),
        Pair("Small habits make big changes.", "James Clear"),
        Pair("Every moment you spend on social media is a step away from your dreams.", "RasFocus+"),
        Pair("Mastering yourself is the greatest power.", "Lao Tzu"),
        Pair("A distracted mind can never achieve great things.", "Rabindranath Tagore")
    )

    private val QUOTES_BY_FEATURE: Map<String, List<Pair<String, String>>> = mapOf(

        "YouTube Shorts" to listOf(
            Pair("Wasting time on Shorts means shrinking your own future.", "RasFocus+"),
            Pair("What you consume defines who you become.", "Cal Newport"),
            Pair("Continuous short-video watching destroys your brain's ability to focus.", "Andrew Huberman"),
            Pair("To dream big, you must give up small distractions.", "Jim Rohn"),
            Pair("Reduce screen time, increase dream time.", "RasFocus+"),
            Pair("The person who respects their time moves forward in life.", "Benjamin Franklin")
        ),

        "Instagram Reels" to listOf(
            Pair("Watching Reels? Your real life is becoming a reel.", "RasFocus+"),
            Pair("Social media is a machine designed to steal your attention.", "Tristan Harris"),
            Pair("Do not dim your own life by watching others' highlight reel.", "RasFocus+"),
            Pair("Every scroll takes away one precious moment of your life.", "Cal Newport"),
            Pair("He who controls others may be powerful, but he who masters himself is mightier still.", "Epictetus"),
            Pair("Your attention is your most valuable asset.", "Robin Sharma")
        ),

        "Facebook Reels" to listOf(
            Pair("Watching Reels? Your real life is becoming a reel.", "RasFocus+"),
            Pair("Facebook wants your time — are you willing to give it?", "RasFocus+"),
            Pair("Every scroll takes away one precious moment of your life.", "Cal Newport"),
            Pair("There is no success without focus.", "Robin Sharma"),
            Pair("We are what we repeatedly do.", "Aristotle")
        ),

        "Facebook Video" to listOf(
            Pair("One video ends, another begins — break this cycle.", "RasFocus+"),
            Pair("To do deep work, you must give up shallow distractions.", "Cal Newport"),
            Pair("Your brain craves rest, not more videos.", "Andrew Huberman"),
            Pair("Time once gone never returns.", "Imam Al-Ghazali"),
            Pair("He who wastes his time wastes himself.", "RasFocus+")
        ),

        "Instagram Stories" to listOf(
            Pair("Write your own story instead of watching others'.", "RasFocus+"),
            Pair("Focus is your most precious asset.", "Robin Sharma"),
            Pair("Stories vanish in 24 hours, but your lost time never returns.", "RasFocus+"),
            Pair("What you watch influences what you think.", "Marcus Aurelius"),
            Pair("Focus on your goals, not on stories.", "RasFocus+")
        ),

        "WhatsApp Channels" to listOf(
            Pair("Communicate only what is necessary, otherwise put the phone down.", "RasFocus+"),
            Pair("Information overload weakens the brain.", "Cal Newport"),
            Pair("Scrolling channels is not productive, it is distraction.", "RasFocus+"),
            Pair("Read less, think more.", "Henry David Thoreau"),
            Pair("Learn what you need to know; everything else is noise.", "RasFocus+")
        ),

        "WhatsApp Status" to listOf(
            Pair("Watching others' status is a waste of time.", "RasFocus+"),
            Pair("Focus on your own work, not on others' lives.", "Marcus Aurelius"),
            Pair("Use the time you spend watching status for yourself.", "RasFocus+"),
            Pair("He who keeps looking at others loses his own path.", "Lao Tzu"),
            Pair("Small habits make big changes.", "James Clear")
        ),

        "WA Business Status" to listOf(
            Pair("Focus on your own work, not on others' lives.", "Marcus Aurelius"),
            Pair("Use the time you spend watching status for yourself.", "RasFocus+"),
            Pair("Time once gone never returns.", "Imam Al-Ghazali"),
            Pair("Small habits make big changes.", "James Clear")
        ),

        "WA Business Channels" to listOf(
            Pair("Information overload weakens the brain.", "Cal Newport"),
            Pair("Read less, think more.", "Henry David Thoreau"),
            Pair("Scrolling channels is not productive, it is distraction.", "RasFocus+"),
            Pair("Discipline is the bridge between goals and accomplishment.", "Jim Rohn")
        ),

        "Snapchat Spotlight" to listOf(
            Pair("Spotlight is stealing your attention.", "RasFocus+"),
            Pair("Continuous short-video watching destroys your brain's ability to focus.", "Andrew Huberman"),
            Pair("To dream big, you must give up small distractions.", "Jim Rohn"),
            Pair("Your time is limited, do not waste it on others' entertainment.", "Steve Jobs"),
            Pair("Attention is the rarest and purest form of generosity — spend it wisely.", "Cal Newport")
        ),

        "Snapchat Stories" to listOf(
            Pair("Write your own story instead of watching others'.", "RasFocus+"),
            Pair("Stories vanish in 24 hours, but your lost time never returns.", "RasFocus+"),
            Pair("Focus on your own work, not on others' lives.", "Marcus Aurelius"),
            Pair("Focus on your goals, not on stories.", "RasFocus+"),
            Pair("He who keeps looking at others loses his own path.", "Lao Tzu")
        ),

        "TikTok" to listOf(
            Pair("TikTok is consuming your hours — stop now.", "RasFocus+"),
            Pair("Break free from the dopamine trap.", "Andrew Huberman"),
            Pair("What you consume defines who you become.", "Cal Newport"),
            Pair("Reduce screen time, increase dream time.", "RasFocus+"),
            Pair("Continuous short videos reduce your brain's ability to focus.", "Andrew Huberman"),
            Pair("To dream big, you must give up small distractions.", "Jim Rohn")
        ),

        "TikTok Live" to listOf(
            Pair("Wasting time watching Lives means wasting your own opportunities.", "RasFocus+"),
            Pair("Your attention is your most valuable asset.", "Robin Sharma"),
            Pair("Instead of watching others' lives, work on your own.", "RasFocus+"),
            Pair("Discipline is the bridge between goals and accomplishment.", "Jim Rohn")
        ),

        "Adult Content" to listOf(
            Pair("Respect yourself — your brain deserves better than this.", "RasFocus+"),
            Pair("What you watch shapes or breaks your character.", "Marcus Aurelius"),
            Pair("Keep your mind pure and your life will be beautiful.", "Imam Al-Ghazali"),
            Pair("Willpower is a muscle — the more you use it, the stronger it gets.", "RasFocus+"),
            Pair("Mastering yourself is the greatest power.", "Lao Tzu"),
            Pair("You are the result of your habits.", "Aristotle")
        ),

        "Google Search" to listOf(
            Pair("Break free from mindless browsing.", "RasFocus+"),
            Pair("Read less, think more.", "Henry David Thoreau"),
            Pair("Information overload weakens the brain.", "Cal Newport"),
            Pair("Learn what you need to know; everything else is noise.", "RasFocus+")
        ),

        "Instagram Search" to listOf(
            Pair("When you open search, do you know where hours disappear?", "RasFocus+"),
            Pair("Before exploring, ask yourself — do you really need this?", "RasFocus+"),
            Pair("Attention is the rarest and purest form of generosity — spend it wisely.", "Cal Newport"),
            Pair("Focus on your own work, not on others' lives.", "Marcus Aurelius")
        ),

        "Unsupported Browser" to listOf(
            Pair("Follow the rules, protect yourself.", "RasFocus+"),
            Pair("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
            Pair("Mastering yourself is the greatest power.", "Epictetus"),
            Pair("Small habits make big changes.", "James Clear")
        ),

        "App Install" to listOf(
            Pair("A new app means a new distraction.", "RasFocus+"),
            Pair("Fewer tools, more focus.", "Cal Newport"),
            Pair("Do not install what you do not need.", "RasFocus+"),
            Pair("Simplicity is the ultimate sophistication.", "Leonardo da Vinci")
        ),

        "Uninstall Blocked" to listOf(
            Pair("You made this decision for your future self — now honor it.", "RasFocus+"),
            Pair("Hold firm to your resolution.", "Marcus Aurelius"),
            Pair("It is easy to break habits in hard times — but that is what sets you back.", "James Clear"),
            Pair("Mastering yourself is the greatest power.", "Epictetus")
        ),

        "Power/Reboot Blocked" to listOf(
            Pair("Your resolve is being tested — this is where you win.", "RasFocus+"),
            Pair("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
            Pair("Persisting through difficulty is true strength.", "Marcus Aurelius"),
            Pair("Mastering yourself is the greatest power.", "Epictetus")
        )
    )

    /** feature নাম দিলে সেই feature-এর random quote দেয়, না পেলে default থেকে দেয় */
    private fun getRandomQuote(featureTitle: String): Pair<String, String> {
        val list = QUOTES_BY_FEATURE[featureTitle] ?: QUOTES_DEFAULT
        return list.random()
    }

    /**
     * featureTitle  → "YouTube Shorts", "Instagram Reels", "Adult Content" ইত্যাদি
     * reason        → কী কারণে block হলো সেটার ছোট বাংলা/English বার্তা
     */
    private var lastPopupTime = 0L

    private fun blockWithMessage(featureTitle: String, reason: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        val now = System.currentTimeMillis()
        if (now - lastPopupTime > 1500L) {   // debounce: 1.5s cooldown
            lastPopupTime = now
            mainHandler.post { showBlockOverlay(featureTitle, reason) }
        }
    }

    private fun blockAndPopup(featureTitle: String, reason: String) = blockWithMessage(featureTitle, reason)

    private fun showBlockOverlay(featureTitle: String, reason: String) {
        val (quoteText, quoteAuthor) = getRandomQuote(featureTitle)
        removeOverlay()

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val ctx = this
        val dp = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels

        // ── Feature → accent color map ──
        val accentHex = when {
            featureTitle.contains("Adult",   true) -> "#FF3B5C"
            featureTitle.contains("Reels",   true) -> "#E1306C"
            featureTitle.contains("Shorts",  true) -> "#FF0000"
            featureTitle.contains("TikTok",  true) -> "#69C9D0"
            featureTitle.contains("Video",   true) -> "#FF6B35"
            featureTitle.contains("Search",  true) -> "#FFAB00"
            featureTitle.contains("Stories", true) -> "#F77737"
            featureTitle.contains("Channel", true) -> "#25D366"
            featureTitle.contains("Status",  true) -> "#25D366"
            featureTitle.contains("Keyword", true) -> "#FF3B5C"
            featureTitle.contains("Install", true) -> "#FFAB00"
            featureTitle.contains("Power",   true) -> "#FF3B5C"
            featureTitle.contains("Reboot",  true) -> "#FF3B5C"
            featureTitle.contains("Uninstall", true) -> "#FF3B5C"
            else -> "#00F5C4"
        }
        val accentColor  = android.graphics.Color.parseColor(accentHex)
        val accentFaded  = android.graphics.Color.parseColor(accentHex.replace("#", "#33"))   // 20% alpha
        val accentFaded2 = android.graphics.Color.parseColor(accentHex.replace("#", "#18"))   // 9% alpha

        // ── Feature icon ──
        val featureIcon = when {
            featureTitle.contains("Adult",    true) -> "🚫"
            featureTitle.contains("Reels",    true) -> "🎬"
            featureTitle.contains("Shorts",   true) -> "📱"
            featureTitle.contains("TikTok",   true) -> "🎵"
            featureTitle.contains("Video",    true) -> "▶"
            featureTitle.contains("Search",   true) -> "🔍"
            featureTitle.contains("Stories",  true) -> "⭕"
            featureTitle.contains("Channel",  true) -> "📢"
            featureTitle.contains("Status",   true) -> "⭕"
            featureTitle.contains("Keyword",  true) -> "⌨"
            featureTitle.contains("Install",  true) -> "📦"
            featureTitle.contains("Power",    true) -> "⚡"
            featureTitle.contains("Reboot",   true) -> "🔄"
            featureTitle.contains("Uninstall",true) -> "🗑"
            else -> "🛡"
        }

        // ════ ROOT — full‑width bottom‑anchored card ════
        val cardWidth = (screenWidth * 0.92f).toInt()
        val hMargin   = ((screenWidth - cardWidth) / 2)

        // Outer container (transparent, just for positioning)
        val outerWrap = android.widget.FrameLayout(ctx)

        // ── Card background: dark gradient + accent top border ──
        val cardBg = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                android.graphics.Color.parseColor("#F0111520"),
                android.graphics.Color.parseColor("#F5090E14")
            )
        ).apply {
            cornerRadius = (24f * dp)
        }

        // Accent top-line (full width, inside card at top)
        val topAccentLine = android.view.View(ctx).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(android.graphics.Color.TRANSPARENT, accentColor, android.graphics.Color.TRANSPARENT)
            )
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (2f * dp).toInt()
            )
        }

        val card = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = cardBg
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // ── Row 1: App badge + dismiss ──
        val row1 = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val appBadgeBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(accentFaded2)
            cornerRadius = 8f * dp
            setStroke((1f * dp).toInt(), accentFaded)
        }
        val appBadge = android.widget.TextView(ctx).apply {
            text = "🛡  RasFocus+"
            textSize = 10.5f
            setTextColor(accentColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
            background = appBadgeBg
            val hp = (10 * dp).toInt(); val vp = (4 * dp).toInt()
            setPadding(hp, vp, hp, vp)
        }
        val spacer1 = android.widget.Space(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, 1, 1f)
        }
        val dismissBtn = android.widget.TextView(ctx).apply {
            text = "✕"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#4B5563"))
            setOnClickListener { removeOverlay() }
            val p = (8 * dp).toInt()
            setPadding(p, p, p, p)
        }
        row1.addView(appBadge)
        row1.addView(spacer1)
        row1.addView(dismissBtn)

        // ── Row 2: Icon + Feature title ──
        val row2 = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, (14 * dp).toInt(), 0, 0) }
            layoutParams = lp
        }
        // Icon circle
        val iconCircleBg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(accentFaded)
        }
        val iconCircle = android.widget.TextView(ctx).apply {
            text = featureIcon
            textSize = 20f
            gravity = Gravity.CENTER
            background = iconCircleBg
            val s = (48 * dp).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(s, s)
        }
        val titleCol = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val lp2 = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins((14 * dp).toInt(), 0, 0, 0) }
            layoutParams = lp2
        }
        val blockedLabel = android.widget.TextView(ctx).apply {
            text = "BLOCKED"
            textSize = 9f
            setTextColor(accentColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.2f
        }
        val featureTitleView = android.widget.TextView(ctx).apply {
            text = featureTitle
            textSize = 17f
            setTextColor(android.graphics.Color.parseColor("#F1F5F9"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, (2 * dp).toInt(), 0, 0)
        }
        titleCol.addView(blockedLabel)
        titleCol.addView(featureTitleView)
        row2.addView(iconCircle)
        row2.addView(titleCol)

        // ── Reason text ──
        val reasonView = android.widget.TextView(ctx).apply {
            text = reason
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            setLineSpacing(0f, 1.4f)
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, (10 * dp).toInt(), 0, 0) }
            layoutParams = lp
        }

        // ── Full-width divider ──
        val divLine = android.view.View(ctx).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#1E2940"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1f * dp).toInt()
            ).apply { setMargins(0, (16 * dp).toInt(), 0, (14 * dp).toInt()) }
        }

        // ── Quote block ──
        val quoteWrap = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        val quoteLine = android.view.View(ctx).apply {
            val lineBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = 4f * dp
            }
            background = lineBg
            layoutParams = android.widget.LinearLayout.LayoutParams(
                (3f * dp).toInt(), android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, 0, (12 * dp).toInt(), 0) }
        }
        val quoteCol2 = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val quoteText2 = android.widget.TextView(ctx).apply {
            text = "\u201C$quoteText\u201D"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#CBD5E1"))
            setTypeface(typeface, android.graphics.Typeface.ITALIC)
            setLineSpacing(0f, 1.4f)
        }
        val authorText = android.widget.TextView(ctx).apply {
            text = "— $quoteAuthor"
            textSize = 10.5f
            setTextColor(accentColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, (5 * dp).toInt(), 0, 0)
        }
        quoteCol2.addView(quoteText2)
        quoteCol2.addView(authorText)
        quoteWrap.addView(quoteLine)
        quoteWrap.addView(quoteCol2)

        // ── Progress bar (countdown visual) ──
        val progressBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor("#1E2940"))
            cornerRadius = 4f * dp
        }
        val progressTrack = android.widget.FrameLayout(ctx).apply {
            background = progressBg
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (4f * dp).toInt()
            ).apply { setMargins(0, (18 * dp).toInt(), 0, 0) }
            layoutParams = lp
        }
        val progressFill = android.view.View(ctx).apply {
            val fillBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = 4f * dp
            }
            background = fillBg
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        progressTrack.addView(progressFill)

        // ── Assemble card ──
        card.addView(topAccentLine)
        card.addView(row1)
        card.addView(row2)
        card.addView(reasonView)
        card.addView(divLine)
        card.addView(quoteWrap)
        card.addView(progressTrack)
        outerWrap.addView(card)

        // ── WindowManager params — bottom center, 92% width ──
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            (screenWidth * 0.92f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (32 * dp).toInt()
        }

        try {
            wm.addView(outerWrap, params)
            overlayView = outerWrap

            // Animate progress bar shrinking over 4 seconds
            val totalMs = 4000L
            val updateMs = 50L
            var elapsed = 0L
            val progressRunnable = object : Runnable {
                override fun run() {
                    elapsed += updateMs
                    val fraction = 1f - (elapsed.toFloat() / totalMs)
                    progressFill.scaleX = fraction.coerceIn(0f, 1f)
                    progressFill.pivotX = 0f
                    if (elapsed < totalMs) {
                        mainHandler.postDelayed(this, updateMs)
                    }
                }
            }
            mainHandler.postDelayed(progressRunnable, updateMs)

            // Auto-dismiss after 4 seconds
            mainHandler.postDelayed({ removeOverlay() }, totalMs)
        } catch (_: Exception) {}
    }

    private fun removeOverlay() {
        overlayView?.let { v ->
            try { windowManager.removeView(v) } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = BlockerPrefs(this)
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ★ FAST PATH — tree scan ছাড়াই event.text থেকে instant block
        event ?: return
        val pkg0 = event.packageName?.toString() ?: return
        if (fastAdultCheck(event, pkg0)) return

        val root = rootInActiveWindow ?: return
        val pkg  = event.packageName?.toString() ?: run { root.recycle(); return }

        try {
            // handled flag — একটা event এ একবারই block হবে, multiple HOME action যাবে না
            var handled = false

            fun block() {
                if (!handled) {
                    handled = true
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }

            // ── Content Blocking ──
            if (!handled) handled = handleAdultContent(root, pkg)
            if (!handled) handleSafeSearch(root, pkg)          // silent inject, never sets handled
            if (!handled) handled = handleWebViewAdultBlock(root, pkg)
            if (!handled) handled = handleImageVideoSearch(root, pkg)
            if (!handled) handled = handleYouTubeShorts(root, pkg)
            if (!handled) handled = handleReels(root, pkg)
            if (!handled) handled = handleInstagramSearch(root, pkg)
            if (!handled) handled = handleWhatsAppChannels(root, pkg)
            if (!handled) handled = handleInstagramStories(root, pkg)
            if (!handled) handled = handleSnapchat(root, pkg)
            if (!handled) handled = handleWhatsAppStatus(root, pkg)
            if (!handled) handled = handleWaBusinessBlocking(root, pkg)
            if (!handled) handled = handleTikTok(root, pkg)
            if (!handled) handled = handleAppSearchKeyword(event, root, pkg)
            if (!handled && pkg == "com.facebook.katana") {
                handled = handleFacebookVideo(root, pkg)
                if (!handled) handled = handleFacebookFeedShortVideo(root, pkg)
            }

            // ── Advanced ──
            if (!handled) handled = handleUnsupportedBrowsers(root, pkg)
            if (!handled) handled = handleNewlyInstalledApps(root, pkg)

            // ── Protection ──
            if (!handled) handled = handleUninstallProtection(root, pkg)
            if (!handled) handleRebootProtection(root, pkg)

        } catch (e: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } finally {
            root.recycle()
        }
    }

    internal fun pauseFeedVideo(root: AccessibilityNodeInfo) {
        val pauseBtn = root
            .findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_pause_button")
            .firstOrNull { it.isVisibleToUser && it.isClickable }
        if (pauseBtn != null) {
            pauseBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            pauseBtn.recycle()
            return
        }
        val videoNode = root
            .findAccessibilityNodeInfosByViewId("com.facebook.katana:id/inline_video_player")
            .firstOrNull { it.isVisibleToUser }
        if (videoNode != null) {
            videoNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            videoNode.recycle()
            return
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onInterrupt() {}

    /** Master service এর কাছ থেকে event receive করার জন্য */
    fun delegateEvent(event: AccessibilityEvent) {
        onAccessibilityEvent(event)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // Switch ON হলে call হয় — already open সাইটও block করে
    fun checkCurrentWindow() {
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: run { root.recycle(); return }
        try {
            handleAdultContent(root, pkg)
        } finally {
            root.recycle()
        }
    }


    // ════════════════════════════════════════════════════════
    // C. ALL BLOCKING HANDLERS
    // ════════════════════════════════════════════════════════

    // ── Helpers ──────────────────────────────────────────────

    // ── All supported browser packages for adult blocking ──
    private val ALL_BROWSER_PKGS = setOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",          // Samsung Internet
        "org.mozilla.firefox",
        "com.microsoft.emmx",                    // Edge
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.UCMobile.intl",
        "com.yandex.browser",
        "com.kiwibrowser.browser",
        "com.vivaldi.browser",
        "mark.via.gp",                           // Via Browser
        "com.duckduckgo.mobile.android",
        "com.mi.globalbrowser",                  // Mi Browser
        "com.hihonor.browser",
        "com.huawei.browser",
        "com.puffin.client.android"
    )

    // ── URL bar view IDs per browser package ──
    private val BROWSER_URL_BAR_IDS = mapOf(
        "com.android.chrome"               to listOf("com.android.chrome:id/url_bar", "com.android.chrome:id/omnibox_text", "com.android.chrome:id/location_bar_edit_text"),
        "com.sec.android.app.sbrowser"     to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text", "com.sec.android.app.sbrowser:id/sb_urlbar_input"),
        "org.mozilla.firefox"              to listOf("org.mozilla.firefox:id/url_bar_title", "org.mozilla.firefox:id/mozac_browser_toolbar_url_view", "org.mozilla.firefox:id/url_edit_text"),
        "com.microsoft.emmx"               to listOf("com.microsoft.emmx:id/address_bar_edit_text", "com.microsoft.emmx:id/url_bar"),
        "com.brave.browser"                to listOf("com.brave.browser:id/url_bar", "com.brave.browser:id/omnibox_text"),
        "com.opera.browser"                to listOf("com.opera.browser:id/url_field"),
        "com.opera.mini.native"            to listOf("com.opera.mini.native:id/url_field"),
        "com.UCMobile.intl"                to listOf("com.UCMobile.intl:id/webview_tab_editurl"),
        "com.yandex.browser"               to listOf("com.yandex.browser:id/bro_urlbar_url"),
        "com.kiwibrowser.browser"          to listOf("com.kiwibrowser.browser:id/url_bar", "com.kiwibrowser.browser:id/omnibox_text"),
        "com.vivaldi.browser"              to listOf("com.vivaldi.browser:id/url_bar", "com.vivaldi.browser:id/omnibox_text"),
        "mark.via.gp"                      to listOf("mark.via.gp:id/cv"),
        "com.duckduckgo.mobile.android"    to listOf("com.duckduckgo.mobile.android:id/omnibarTextInput"),
        "com.mi.globalbrowser"             to listOf("com.mi.globalbrowser:id/url_address"),
        "com.hihonor.browser"              to listOf("com.hihonor.browser:id/url_edit_text"),
        "com.huawei.browser"               to listOf("com.huawei.browser:id/url_edit_text"),
        "com.puffin.client.android"        to listOf("com.puffin.client.android:id/url_edit_text")
    )

    private fun extractBrowserUrl(root: AccessibilityNodeInfo, pkg: String): String? {
        // Try package-specific IDs first
        val ids = BROWSER_URL_BAR_IDS[pkg]
        if (ids != null) {
            for (id in ids) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (nodes.isNotEmpty()) {
                    val t = nodes[0].text?.toString()?.lowercase()
                        ?: nodes[0].contentDescription?.toString()?.lowercase()
                    if (!t.isNullOrBlank()) return t
                }
            }
        }
        // Fallback: tree scan for URL-shaped text
        return findNodeWithUrl(root)
    }

    // Keep old name as alias for backward compat with other handlers
    private fun extractChromeUrl(root: AccessibilityNodeInfo): String? =
        extractBrowserUrl(root, "com.android.chrome")

    private fun findNodeWithUrl(node: AccessibilityNodeInfo): String? {
        val t = node.text?.toString()?.lowercase() ?: ""
        if (t.startsWith("http") || t.startsWith("www.") || t.contains(".com")) return t
        for (i in 0 until node.childCount) {
            val r = findNodeWithUrl(node.getChild(i) ?: continue)
            if (r != null) return r
        }
        return null
    }

    private fun isTabActive(root: AccessibilityNodeInfo, name: String) =
        root.findAccessibilityNodeInfosByText(name)
            .any { it.isSelected || it.isChecked || it.isFocused }

    private val BLOCKED_BROWSERS = setOf(
        "com.opera.browser", "com.opera.mini.native", "com.UCMobile.intl",
        "org.mozilla.firefox", "com.brave.browser", "com.microsoft.emmx",
        "com.duckduckgo.mobile.android", "com.yandex.browser",
        "com.kiwibrowser.browser", "com.vivaldi.browser", "mark.via.gp",
        "com.puffin.client.android", "com.jawal.browser"
    )
    private val BLOCKED_BROWSER_NAMES = listOf(
        "Opera", "Opera Mini", "UC Browser", "Firefox", "Brave",
        "Edge", "DuckDuckGo", "Yandex Browser", "Kiwi", "Vivaldi",
        "Via Browser", "Puffin"
    )

    private val rasFocusPackage = "com.rasfocus"
    private val rasFocusName = "RasFocus"

    // ── Adult site domain list (assets/adultsite.txt থেকে lazy load) ──
    private var _adultDomainList: List<String>? = null
    private fun getAdultDomainList(): List<String> {
        if (_adultDomainList == null) {
            _adultDomainList = try {
                assets.open("adultsite.txt")
                    .bufferedReader()
                    .readLines()
                    .map { it.trim().trimStart('*', '.').lowercase() }
                    .filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }
        return _adultDomainList!!
    }

    // ── 1. Adult Content ─────────────────────────────────────
    // ── Adult site keywords — adultsite.txt + Adult_block.kt সব keywords ──
    private val adultSiteKeywords = listOf(
        // ── Hardcore keywords (EN) ──
        "porn", "xxx", "nude", "nsfw", "sexy", "hentai", "rule34", "milf",
        "blowjob", "tits", "boobs", "pussy", "dick", "cock", "escort", "bdsm",
        "fetish", "erotica", "dildo", "webcam", "camgirls", "onlyfans", "chaturbate",
        "mia khalifa", "sunny leone", "dani daniels", "johnny sins", "kendra lust",
        // ── Romantic/Soft keywords ──
        "hot dance", "seductive dance", "item song", "belly dance",
        "kissing scene", "bikini", "swimsuit", "sexy dance", "cleavage", "hot scene",
        "romantic kiss", "bedroom scene", "bath scene", "rain dance", "bold scene",
        "semi nude", "lingerie", "erotic", "hot song", "romantic video hot",
        "navel show", "deep neck", "short dress sexy", "unfaithful scene",
        // ── Bangla keywords ──
        "চটি", "পর্ণ", "সেক্স", "নগ্ন", "উলঙ্গ", "বেশ্যা", "মাগি", "খানকি",
        "যৌন", "পর্ণগ্রাফি", "রেন্ডি", "চোদাচুতি", "গরম ভিডিও", "খারাপ ছবি",
        "যৌন মিলন", "যৌনাঙ্গ", "চুদো", "নগ্নতা",
        // ── Domain keywords (adultsite.txt) ──
        "18videosz", "24porn", "3movs", "4tube", "adulttime", "allofgfs",
        "alohatube", "alotporn", "alphaporno", "anyshemale", "arabianchicks",
        "baberotica", "badoinkvr", "bangbrosnetwork", "bdsmstreak", "beeg",
        "bestpornbabes", "besttrannypornsites", "bestxxxsites", "bigtits",
        "blacked", "braincash", "brazzers", "brokestraightboys",
        "camhub", "cliphunter", "clips4sale", "czechvr", "dansmovies",
        "daredorm", "ddfnetwork", "deviantclip", "digitalplayground", "dorcelclub",
        "drtuber", "eggporncomics", "empflix", "eporner", "eroxia",
        "evilangel", "extremetube", "fakehub", "fakku", "fantasti",
        "fapster", "forhertube", "free18", "freepornfull",
        "gayfuror", "gaymaletube", "gaytube", "gelbooru", "gfrevenge",
        "girlsway", "gotgayporn", "h2porn", "handjobhub", "hardsextube",
        "hclips", "helixstudios", "hentaigasm", "hentaihaven", "hentaipulse",
        "hentai", "hotgoo", "hotsouthindiansex", "hustler", "iknowthatgirl",
        "imlive", "japanhdv", "javhd", "jerkmate", "jizzhut", "jizzonline",
        "justusboys", "keezmovies", "kinkyfamily", "kporno",
        "lesbian8", "letsjerk", "lovehomeporn", "lubetube", "luckycrush",
        "madthumbs", "manporn", "maxiporn", "metaporn", "mofosex",
        "mogosnetwork", "motherless", "moviefap", "myporngay", "netfapx",
        "newsensations", "nonktube", "nubiles", "nuvid",
        "perfectgirls", "perfectgonzo", "pervclips", "playboy", "porcore",
        "porn", "porn300", "porn7", "porndroids", "pornerbros", "pornfuror",
        "pornhd", "pornheed", "pornhost", "pornhub", "pornhubselect",
        "pornmate", "pornmd", "pornmilo", "pornotube", "pornoxo",
        "pornprosnetwork", "pornrabbit", "pornrox", "pornstarnetwork",
        "porntube", "pornxio", "proporn", "punishbang", "punishtube",
        "realitykings", "redgifs", "redporn", "redtube", "rockettube",
        "sankakucomplex", "sexlikereal", "sexvid", "shooshtime",
        "slutload", "slutroulette", "spankbang", "spankwire", "stripchat",
        "submityourflicks", "submityourtapes", "sunporno", "teamskeet",
        "theporndude", "thumbzilla", "tiava", "tnaflix", "topfreepornvideos",
        "toppornsites", "tranny", "tube8", "tubegalore", "tubegals",
        "twilightsex", "twistysnetwork", "txxx", "videosz", "viewdesisex",
        "virtualtaboo", "vixen", "vporn", "vrcock", "vrcosplay",
        "vrporn", "vrsmash", "wankzvr", "watchindianporn", "watchmyexgf",
        "watchmygf", "xbabe", "xhamster", "xmoviesforyou", "xnxx",
        "xnxxhamster", "xpaja", "xtube", "xvideos", "xxvids",
        "xxxaporn", "xxxbunker", "xxxvideos247", "youjizz", "youporn",
        "youporngay", "yuvutu", "zbporn", "zzcartoon", "zzgays",
        // general keywords
        "porn", "xxx", "nude", "nsfw", "sex", "hentai"
    )

    // Full domain set — exact host match
    private val adultDomains = setOf(
        "18videosz.com", "24porn.com", "3movs.com", "4tube.com",
        "adulttime.com", "allofgfs.com", "alohatube.com", "alotporn.com",
        "alphaporno.com", "anon-v.com", "anyshemale.com", "arabianchicks.com",
        "avn.com", "baberotica.com", "babes.com", "badoinkvr.com",
        "bang.com", "bangbrosnetwork.com", "bdsmstreak.com", "beeg.com",
        "bestpornbabes.com", "besttrannypornsites.com", "bestxxxsites.com", "bigtits.com",
        "blacked.com", "bobs-tube.com", "boysfood.com", "braincash.com",
        "brazzers.com", "brokestraightboys.com", "camhub.cc", "cams.com",
        "cliphunter.com", "clips4sale.com", "czechvr.com", "dansmovies.com",
        "daredorm.com", "ddfnetwork.com", "deviantclip.com", "digitalplayground.com",
        "dorcelclub.com", "drtuber.com", "eggporncomics.com", "empflix.com",
        "eporner.com", "eroxia.com", "evilangel.com", "extremetube.com",
        "fakehub.com", "fakku.net", "fantasti.cc", "fapster.xxx",
        "forhertube.com", "free18.net", "freepornfull.com", "fuq.com",
        "fux.com", "gayfuror.com", "gaymaletube.com", "gaytube.com",
        "gelbooru.com", "gfrevenge.com", "girlsway.com", "gotgayporn.com",
        "h2porn.com", "handjobhub.com", "hardsextube.com", "hclips.com",
        "helixstudios.net", "hentai-foundry.com", "hentaicore.org", "hentaigasm.com",
        "hentaihaven.org", "hentaipulse.com", "hotgoo.com", "hotsouthindiansex.com",
        "hustler.com", "iknowthatgirl.com", "imlive.com", "ixxx.com",
        "iyalc.com", "japanhdv.com", "javhd.com", "jerkmate.com",
        "jizzhut.com", "jizzonline.com", "justusboys.com", "keezmovies.com",
        "kinkyfamily.com", "kporno.com", "lesbian8.com", "letsjerk.is",
        "lovehomeporn.com", "lubetube.com", "luckycrush.live", "madthumbs.com",
        "manporn.xxx", "maxim.com", "maxiporn.com", "metaporn.com",
        "mofosex.com", "mogosnetwork.com", "motherless.com", "moviefap.com",
        "myporngay.com", "mythav.com", "netfapx.com", "newsensations.com",
        "nonktube.com", "nubiles.net", "nuvid.com", "orgasm.com",
        "perfectgirls.net", "perfectgonzo.com", "pervclips.com", "playboy.com",
        "porcore.com", "porn.com", "porn300.xxx", "porn7.xxx",
        "porndroids.com", "pornerbros.com", "pornfuror.com", "pornhd.com",
        "pornheed.com", "pornhost.com", "pornhub.com", "pornhubselect.com",
        "pornmate.com", "pornmd.com", "pornmilo.com", "pornotube.com",
        "pornoxo.com", "pornprosnetwork.com", "pornrabbit.com", "pornrox.com",
        "pornstarnetwork.com", "porntube.com", "pornxio.com", "proporn.com",
        "punishbang.com", "punishtube.com", "realitykings.com", "redgifs.com",
        "redporn.xxx", "redtube.com", "rk.com", "rockettube.com",
        "rude.com", "sankakucomplex.com", "sexlikereal.com", "sexvid.xxx",
        "shameless.com", "shemailhd.sex", "shooshtime.com", "slutload.com",
        "slutroulette.com", "spankbang.com", "spankwire.com", "stripchat.com",
        "submityourflicks.com", "submityourtapes.com", "sunporno.com", "teamskeet.com",
        "theporndude.com", "thumbzilla.com", "tiava.com", "tnaflix.com",
        "topfreepornvideos.com", "toppornsites.com", "tranny.one", "tube8.com",
        "tubegalore.com", "tubegals.com", "tubev.sex", "twilightsex.com",
        "twistysnetwork.com", "txxx.com", "videosz.com", "viewdesisex.com",
        "virtualtaboo.com", "vixen.com", "vporn.com", "vrcock.com",
        "vrcosplay.com", "vrporn.com", "vrsmash.com", "wankzvr.com",
        "watch-my-gf.com", "watch-my-gf.me", "watchindianporn.net", "watchmyexgf.net",
        "watchmygf.me", "watchmygf.tv", "xbabe.com", "xhamster.com",
        "xmoviesforyou.com", "xnxx.com", "xnxxhamster.net", "xpaja.net",
        "xtube.com", "xvideos.com", "xxvids.net", "xxx.com",
        "xxxaporn.com", "xxxbunker.com", "xxxvideos247.com", "youjizz.com",
        "youporn.com", "youporngay.com", "yuvutu.com", "zbporn.com",
        "zzcartoon.com", "zzgays.com"
    )

    // ── FAST PATH — event.text থেকে সরাসরি, tree scan ছাড়াই ──
    private fun fastAdultCheck(event: AccessibilityEvent, pkg: String): Boolean {
        if (pkg !in ALL_BROWSER_PKGS) return false
        if (!prefs.blockAdult && !prefs.blockAdultSiteList) return false
        val eventText = event.text.joinToString(" ").lowercase()
        if (eventText.isBlank()) return false
        // SafeSearch bypass attempt — fast block
        if (prefs.blockAdult && (eventText.contains("safe=off") || eventText.contains("safe=images"))) {
            blockAdultInBrowser(pkg)
            return true
        }
        if (isAdultUrl(eventText)) {
            blockAdultInBrowser(pkg)
            return true
        }
        return false
    }

    /**
     * Adult content detected in a browser:
     * 1. Show blocking popup immediately
     * 2. Press BACK to close/navigate away from the blocked tab
     * 3. Open a clean Google tab in Chrome (or just go home for non-Chrome browsers)
     */
    private fun blockAdultInBrowser(pkg: String) {
        // Show popup overlay
        mainHandler.post { showBlockOverlay("Adult Content", "This page contains adult content and has been blocked.") }

        // Navigate away: BACK closes the current tab in most browsers
        performGlobalAction(GLOBAL_ACTION_BACK)

        // For Chrome/Samsung: open google.com in a new tab after a short delay
        if (pkg == "com.android.chrome" || pkg == "com.sec.android.app.sbrowser") {
            mainHandler.postDelayed({
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage(pkg)
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }, 400)
        } else {
            // For other browsers, just go home after popup
            mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 400)
        }
    }

    private fun isAdultUrl(text: String): Boolean {
        if (prefs.blockAdult) {
            if (adultSiteKeywords.any { text.contains(it) }) return true
        }
        if (prefs.blockAdult || prefs.blockAdultSiteList) {
            val host = try {
                val raw = if (text.startsWith("http")) text.trim() else "https://${text.trim()}"
                android.net.Uri.parse(raw).host?.lowercase()?.removePrefix("www.") ?: ""
            } catch (e: Exception) { "" }
            if (host.isNotEmpty()) {
                // Check hardcoded domain set
                if (adultDomains.contains(host)) return true
                // Check adultsite.txt domain list
                if (prefs.blockAdultSiteList) {
                    val domainList = getAdultDomainList()
                    if (domainList.any { host == it || host.endsWith(".$it") }) return true
                }
            }
        }
        return false
    }

    // ── SCREEN TEXT HELPER ──
    private fun collectAllText(node: AccessibilityNodeInfo?): String {
        node ?: return ""
        val sb = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?) {
            n ?: return
            n.text?.let { sb.append(it).append(" ") }
            n.contentDescription?.let { sb.append(it).append(" ") }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(node)
        return sb.toString()
    }

    private fun handleAdultContent(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (pkg !in ALL_BROWSER_PKGS) return false
        if (!prefs.blockAdult && !prefs.blockAdultSiteList) return false

        val url = extractBrowserUrl(root, pkg)?.lowercase() ?: ""
        val check = if (url.isNotBlank()) url else collectAllText(root).lowercase()
        if (check.isBlank()) return false

        if (isAdultUrl(check)) {
            blockAdultInBrowser(pkg)
            return true
        }
        return false
    }

    // ── 1b. SafeSearch Force ─────────────────────────────────
    // blockAdult ON থাকলে Google search-এ safe=active inject করে
    // safe=off bypass attempt ও block করে
    private val safeSearchLastRedirect = HashMap<String, Long>()

    private fun handleSafeSearch(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockAdult) return false
        if (pkg !in ALL_BROWSER_PKGS) return false

        val url = extractBrowserUrl(root, pkg)?.lowercase() ?: return false
        if (!url.contains("google.") || !url.contains("/search")) return false

        // SafeSearch bypass attempt — block
        if (url.contains("safe=off") || url.contains("safe=images")) {
            blockAdultInBrowser(pkg)
            return true
        }

        // Already safe — skip
        if (url.contains("safe=active") || url.contains("safe=strict")) return false

        // Throttle: 1.5s cooldown per pkg to avoid redirect loop
        val now = System.currentTimeMillis()
        if ((now - (safeSearchLastRedirect[pkg] ?: 0L)) < 1500L) return false
        safeSearchLastRedirect[pkg] = now

        // Build safe URL and redirect
        val safeUrl = url + (if (url.contains("?")) "&" else "?") + "safe=active"
        mainHandler.postDelayed({
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(safeUrl)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                             android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    setPackage(pkg)
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }, 300)
        return false  // don't mark handled — just redirect silently
    }

    // ── 1c. WebView Adult Block ─────────────────────────────
    // Any app using WebView to load adult content — block it
    private fun handleWebViewAdultBlock(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockAdult && !prefs.blockAdultSiteList) return false
        if (pkg in ALL_BROWSER_PKGS) return false  // already handled
        if (pkg.startsWith("com.android.") || pkg == "android") return false

        // Detect WebView presence
        if (!containsWebViewNode(root)) return false

        // Get URL or screen text from WebView
        val urlText = findNodeWithUrl(root)?.lowercase() ?: ""
        val checkText = if (urlText.isNotBlank()) urlText else collectAllText(root).lowercase()
        if (checkText.isBlank()) return false

        if (isAdultUrl(checkText)) {
            mainHandler.post {
                showBlockOverlay("Adult Content", "This page contains adult content and has been blocked.")
            }
            performGlobalAction(GLOBAL_ACTION_BACK)
            return true
        }
        return false
    }

    private fun containsWebViewNode(node: AccessibilityNodeInfo?): Boolean {
        node ?: return false
        val cn = node.className?.toString() ?: ""
        if (cn.contains("WebView") || cn.contains("XWalkView")) return true
        for (i in 0 until node.childCount) {
            if (containsWebViewNode(node.getChild(i))) return true
        }
        return false
    }

    // ── 2. Image & Video Search ──────────────────────────────
    private fun handleImageVideoSearch(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockSearch) return false
        if (pkg != "com.android.chrome") return false
        val url = extractChromeUrl(root) ?: ""
        // URL-based block
        val urlBlocked = url.contains("tbm=isch") || url.contains("tbm=vid")
            || url.contains("google.com/images") || url.contains("google.com/videohp")
        // Tab-based block (URL না পেলেও tab active কিনা দেখো)
        val tabBlocked = url.isEmpty() && (isTabActive(root, "Images") || isTabActive(root, "Videos"))
        if (urlBlocked || tabBlocked) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return true
        }
        return false
    }

    // ── 3. YouTube Shorts ────────────────────────────────────
    private fun handleYouTubeShorts(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockYtShorts) return false
        if (pkg != "com.google.android.youtube") return false

        val tab = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/pivot_bar_item_label")
            .any { it.text?.toString()?.equals("Shorts", true) == true && (it.isSelected || it.parent?.isSelected == true) }
        if (tab) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

        val player = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/shorts_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_player_page").isNotEmpty()
        if (player) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

        val via = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_while_layout").isNotEmpty()
            && root.findAccessibilityNodeInfosByText("Shorts").any { it.isSelected }
        if (via) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

        val fb = root.findAccessibilityNodeInfosByText("Shorts")
            .any { it.isSelected || it.isChecked || it.parent?.isSelected == true }
        if (fb) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

        return false
    }

    // ── 4. Reels ─────────────────────────────────────────────
    private fun handleReels(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockReels) return false
        when (pkg) {
            "com.instagram.android" -> {
                if (root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_tab").isNotEmpty()) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
                if (root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_viewer_container").isNotEmpty()) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
                if (root.findAccessibilityNodeInfosByText("Reels")
                        .any { it.contentDescription?.contains("Reels") == true && it.isSelected }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
                if (isTabActive(root, "Reels")) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            "com.facebook.katana" -> {
                // blockReels toggle — শুধু actual reels viewer খোলা হলে block করো
                if (isFbReelsViewerOpen(root)) {
                    blockFacebookContent("Facebook Reels", "Facebook Reels is blocked.")
                    return true
                }
            }
        }
        return false
    }

    // ── 5. Instagram Search ──────────────────────────────────
    private fun handleInstagramSearch(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockInstaSearch) return false
        if (pkg != "com.instagram.android") return false
        if (root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/search_tab").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        if (root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/action_bar_search_edit_text")
                .any { it.isFocused || it.isAccessibilityFocused }) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        if (root.findAccessibilityNodeInfosByText("Search and explore").isNotEmpty()
            || root.findAccessibilityNodeInfosByText("Search").any { it.isSelected }) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        return false
    }

    // ── 6. WhatsApp Channels ─────────────────────────────────
    private fun handleWhatsAppChannels(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockWaChannels) return false
        if (pkg != "com.whatsapp") return false
        if (root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/updates_tab").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        if (root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/channels_home_content_layout").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        if (root.findAccessibilityNodeInfosByText("Channels").any { it.isSelected || it.isChecked }) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        if (root.findAccessibilityNodeInfosByText("Updates").any { it.isSelected || it.isChecked }) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        return false
    }

    // ── Facebook block helper — HOME-এ না পাঠিয়ে FB homepage reload করে + popup দেখায় ──
    private var fbBlockLastTime = 0L

    private fun blockFacebookContent(featureTitle: String, reason: String) {
        val now = System.currentTimeMillis()
        if (now - fbBlockLastTime < 1200L) return  // debounce — rapid firing রোধ করো
        fbBlockLastTime = now

        // Popup দেখাও
        mainHandler.post { showBlockOverlay(featureTitle, reason) }

        // Facebook news feed এ ফিরে যাও (HOME-এ না)
        mainHandler.postDelayed({
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    setPackage("com.facebook.katana")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                             android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            } catch (_: Exception) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }, 300)
    }

    // ── Facebook: আসলেই video player খোলা হয়েছে কিনা detect করো ──
    // Feed-এ শুধু scroll করার সময় inline_video_player visible হয় — সেটা block করা উচিত না
    // কিন্তু fullscreen player বা reels viewer খুললে — block করো
    private fun isFbVideoPlayerActuallyOpen(root: AccessibilityNodeInfo): Boolean {
        // Fullscreen / dedicated video player — এগুলো শুধু video খুললেই আসে
        val fullscreen =
            root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/fullscreen_video_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/fb_video_player").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_player_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_scrubber").isNotEmpty()  // scrubber মানে video খোলা
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_control_overlay").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_controls_root").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/inline_video_controller").isNotEmpty()
        if (fullscreen) return true

        // Story video player
        val story =
            root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/story_viewer_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_story_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/story_video_player").isNotEmpty()
        if (story) return true

        // Mute/unmute + pause আছে মানে fullscreen video চলছে (feed-এ এগুলো একসাথে আসে না)
        val hasScrubberOrControls =
            root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_unmute_button").any { it.isVisibleToUser }
            && root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/video_pause_button").any { it.isVisibleToUser }
        if (hasScrubberOrControls) return true

        return false
    }

    // ── Facebook Reels: feed tray নয়, actual reels viewer খোলা হয়েছে কিনা ──
    private fun isFbReelsViewerOpen(root: AccessibilityNodeInfo): Boolean {
        // Reels tab বা viewer — এগুলো শুধু reels section-এ গেলেই আসে
        return root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/reels_viewer_root").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/reels_viewer_fragment").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/reels_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/short_video_player").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/clip_player_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/fb_shorts_container").isNotEmpty()
            // Reels tab button selected মানে reels section-এ আছে
            || root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/reels_tab_button")
                .any { it.isSelected || it.isChecked || it.parent?.isSelected == true }
            // Feed-এর reels tray scroll করতে গেলে নয়, reels_tray_item click হলে
            || (root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/reels_tray_container").isNotEmpty()
                && root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/reels_viewer_root").isNotEmpty())
    }

    // ── 7. Facebook Video ────────────────────────────────────
    private fun handleFacebookVideo(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockFbVideo) return false

        // Watch tab — dedicated video section
        if (root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/watch_tab").isNotEmpty()
            || root.findAccessibilityNodeInfosByText("Watch").any { it.isSelected || it.parent?.isSelected == true }) {
            blockFacebookContent("Facebook Video", "Facebook Watch tab is blocked.")
            return true
        }

        // Actual video player open হয়েছে — block করো
        if (isFbVideoPlayerActuallyOpen(root)) {
            blockFacebookContent("Facebook Video", "Facebook video player is blocked.")
            return true
        }

        return false
    }

    // ── 8. Facebook Feed Reels ───────────────────────────────
    private fun handleFacebookFeedShortVideo(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockFbVideo && !prefs.blockReels) return false

        // Actual Reels viewer খোলা হয়েছে — block করো
        // Feed scroll করার সময় reels tray দেখা গেলে block করবে না
        if (isFbReelsViewerOpen(root)) {
            blockFacebookContent("Facebook Reels", "Facebook Reels is blocked.")
            return true
        }

        // blockFbVideo: autoplay video in feed (mute+pause একসাথে ছাড়া শুধু autoplay signal)
        if (prefs.blockFbVideo) {
            val autoplay = root.findAccessibilityNodeInfosByViewId("com.facebook.katana:id/auto_play_video")
                .any { it.isVisibleToUser }
            if (autoplay) {
                blockFacebookContent("Facebook Video", "Facebook auto-play video is blocked.")
                return true
            }
        }

        return false
    }


    // ── 8b. Instagram Stories Block ──────────────────────────
    private fun handleInstagramStories(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockInstaStories) return false
        if (pkg != "com.instagram.android") return false
        // L1: Stories tray / container
        if (root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/story_container_layout").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/stories_viewer_fragment").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_header_overlay_fragment").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        // L2: Text-based detection
        if (root.findAccessibilityNodeInfosByText("Story").any { it.isVisibleToUser && it.isClickable }
            || root.findAccessibilityNodeInfosByText("Your story").any { it.isVisibleToUser }) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        // L3: Content description fallback
        if (root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_video_see_more_layout").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/story_viewer_container").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        return false
    }

    // ── 8c. Snapchat Spotlight & Stories Block ────────────────
    private fun handleSnapchat(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (pkg != "com.snapchat.android") return false

        // Spotlight block
        if (prefs.blockSnapSpotlight) {
            // L1: Spotlight tab/container
            if (root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/spotlight_tab").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/discover_feed_container").isNotEmpty()
                || root.findAccessibilityNodeInfosByText("Spotlight").any { it.isSelected || it.parent?.isSelected == true }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
            // L2: Content description / bottom nav
            if (root.findAccessibilityNodeInfosByText("Spotlight").any { it.isVisibleToUser && it.isClickable }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
        }

        // Stories block
        if (prefs.blockSnapStories) {
            // L1: Stories viewer
            if (root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/story_viewer_container").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/stories_feed_container").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/top_snap_container").isNotEmpty()) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
            // L2: Friends stories row/tab
            if (root.findAccessibilityNodeInfosByText("Stories").any { it.isSelected || it.isChecked }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
        }

        return false
    }

    // ── 8d. WhatsApp Status Block ─────────────────────────────
    private fun handleWhatsAppStatus(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockWaStatus) return false
        if (pkg != "com.whatsapp") return false
        // L1: Status tab
        if (root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status_tab").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status_list").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status_view_page_indicator").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        // L2: Text-based (Updates tab এ Status section)
        if (root.findAccessibilityNodeInfosByText("Status").any { it.isSelected || it.isChecked }) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        // L3: Status viewer
        if (root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status_viewer_fragment_container").isNotEmpty()
            || root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status_header_text_view").isNotEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK); return true
        }
        return false
    }

    // ── 8e. WhatsApp Business Status & Channels Block ─────────
    private fun handleWaBusinessBlocking(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (pkg != "com.whatsapp.w4b") return false

        if (prefs.blockWaBusinessStatus) {
            if (root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/status_tab").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/status_list").isNotEmpty()
                || root.findAccessibilityNodeInfosByText("Status").any { it.isSelected || it.isChecked }) {
                performGlobalAction(GLOBAL_ACTION_BACK); return true
            }
        }

        if (prefs.blockWaBusinessChannels) {
            if (root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/updates_tab").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/channels_home_content_layout").isNotEmpty()
                || root.findAccessibilityNodeInfosByText("Channels").any { it.isSelected || it.isChecked }
                || root.findAccessibilityNodeInfosByText("Updates").any { it.isSelected || it.isChecked }) {
                performGlobalAction(GLOBAL_ACTION_BACK); return true
            }
        }

        return false
    }

    // ── 8f. TikTok Block ──────────────────────────────────────
    private fun handleTikTok(root: AccessibilityNodeInfo, pkg: String): Boolean {
        val tiktokPkgs = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.tiktok.android")
        if (pkg !in tiktokPkgs) return false

        if (prefs.blockTikTok) {
            // L1: For You / Following feed (main screen)
            if (root.findAccessibilityNodeInfosByViewId("$pkg:id/tt_feed_item").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("$pkg:id/feed_container").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("$pkg:id/main_feed_page_video").isNotEmpty()) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
            // L2: Any TikTok screen (broad block)
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }

        if (prefs.blockTikTokLive) {
            if (root.findAccessibilityNodeInfosByViewId("$pkg:id/live_container").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("$pkg:id/live_room_fragment").isNotEmpty()
                || root.findAccessibilityNodeInfosByText("LIVE").any { it.isVisibleToUser }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
        }

        return false
    }

    // ── 8g. App Search Keyword Block ─────────────────────────
    // Telegram, WhatsApp, Facebook search এ adult keyword type করলে
    // typing শেষ হওয়ার আগেই block + search field clear
    private fun handleAppSearchKeyword(
        event: AccessibilityEvent?,
        root: AccessibilityNodeInfo,
        pkg: String
    ): Boolean {
        val targetPkgs = setOf(
            "org.telegram.messenger",        // Telegram
            "org.telegram.messenger.web",
            "com.whatsapp",                  // WhatsApp
            "com.whatsapp.w4b",              // WA Business
            "com.facebook.katana",           // Facebook
            "com.facebook.lite"              // FB Lite
        )
        if (pkg !in targetPkgs) return false

        // TYPE_VIEW_TEXT_CHANGED — user যা type করছে সেটা real-time চেক
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return false

        // event.text থেকে typed text নাও
        val typedText = event.text.joinToString(" ").lowercase().trim()
        if (typedText.isBlank()) return false

        // keyword match check
        val matched = adultSiteKeywords.any { kw -> typedText.contains(kw) }
        if (!matched) return false

        // ── Search field clear করো ──
        try {
            val source = event.source
            if (source != null) {
                // Method 1: ACTION_SET_TEXT দিয়ে empty করো
                val args = android.os.Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, ""
                    )
                }
                val cleared = source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (!cleared) {
                    // Method 2: select all + cut fallback
                    val sel = android.os.Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, typedText.length)
                    }
                    source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel)
                    source.performAction(AccessibilityNodeInfo.ACTION_CUT)
                }
                source.recycle()
            }
        } catch (e: Exception) {
            // clear fail হলেও block চলবে
        }

        // HOME এ পাঠাও + overlay দেখাও
        blockWithMessage("Adult Content", "Blocked keyword typed in search")
        return true
    }

    // ── 9. Unsupported Browsers — 5 Layers ───────────────────
    private fun handleUnsupportedBrowsers(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockUnsupported) return false
        // L1: package match
        if (BLOCKED_BROWSERS.contains(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        // L2: recent apps
        val recentPkgs = setOf("com.android.systemui", "com.samsung.android.systemui", "com.miui.systemui")
        if (recentPkgs.contains(pkg)) {
            val inRecents = BLOCKED_BROWSER_NAMES.any { n -> root.findAccessibilityNodeInfosByText(n).any { it.isVisibleToUser } }
            if (inRecents) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
        }
        // L3: Settings app info of blocked browser
        if (pkg == "com.android.settings") {
            val header = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/entity_header_title")
                .firstOrNull { it.isVisibleToUser }?.text?.toString() ?: ""
            if (BLOCKED_BROWSER_NAMES.any { header.contains(it, true) }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
            val defaultBrowserSec = root.findAccessibilityNodeInfosByText("Browser app").any { it.isVisibleToUser }
                || root.findAccessibilityNodeInfosByText("Default browser").any { it.isVisibleToUser }
            if (defaultBrowserSec && BLOCKED_BROWSER_NAMES.any { n -> root.findAccessibilityNodeInfosByText(n).any { it.isClickable || it.isSelected } }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
        }
        // L4: intent chooser
        val chooserVisible = root.findAccessibilityNodeInfosByViewId("com.android.intentresolver:id/chooser_list").any { it.isVisibleToUser }
            || root.findAccessibilityNodeInfosByViewId("android:id/resolver_list").any { it.isVisibleToUser }
            || root.findAccessibilityNodeInfosByText("Open with").any { it.isVisibleToUser }
        if (chooserVisible && BLOCKED_BROWSER_NAMES.any { n -> root.findAccessibilityNodeInfosByText(n).any { it.isVisibleToUser } }) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        // L5: notification deep link
        val notif = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/notification_panel").any { it.isVisibleToUser }
        if (notif && BLOCKED_BROWSER_NAMES.any { n -> root.findAccessibilityNodeInfosByText(n).any { it.isVisibleToUser } }) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        return false
    }

    // ── 10. New App Install — 5 Layers ───────────────────────
    private fun handleNewlyInstalledApps(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockNewApps) return false
        val installerPkgs = setOf("com.android.packageinstaller", "com.google.android.packageinstaller")
        // L1: stock installer
        if (installerPkgs.contains(pkg)) {
            val confirm = root.findAccessibilityNodeInfosByText("Do you want to install this app?").isNotEmpty()
                || root.findAccessibilityNodeInfosByText("Install").any { it.isClickable && it.isVisibleToUser }
            if (confirm) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            val panel = root.findAccessibilityNodeInfosByViewId("com.android.packageinstaller:id/install_confirm_panel")
                .any { it.isVisibleToUser }
            if (panel) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            val anyway = root.findAccessibilityNodeInfosByText("Install anyway").any { it.isClickable }
                || root.findAccessibilityNodeInfosByViewId("com.android.packageinstaller:id/install_button")
                    .any { it.isVisibleToUser }
            if (anyway) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
        }
        // L2: Play Store install/update
        if (pkg == "com.android.vending") {
            val install = root.findAccessibilityNodeInfosByViewId("com.android.vending:id/buy_button")
                .any { it.isVisibleToUser && it.isClickable }
                || root.findAccessibilityNodeInfosByText("Install").any { it.isClickable && it.isVisibleToUser }
            if (install) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            val update = root.findAccessibilityNodeInfosByText("Update").any { it.isClickable && it.isVisibleToUser }
                || root.findAccessibilityNodeInfosByViewId("com.android.vending:id/update_button")
                    .any { it.isVisibleToUser }
            if (update) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            val updateAll = root.findAccessibilityNodeInfosByText("Update all").any { it.isClickable }
            if (updateAll) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            // L3: post-install Open button
            val openAfter = root.findAccessibilityNodeInfosByText("Open").any { it.isClickable && it.isVisibleToUser }
                && root.findAccessibilityNodeInfosByText("Uninstall").any { it.isVisibleToUser }
            if (openAfter) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
        }
        // L3b: package installer complete screen
        if (installerPkgs.contains(pkg)) {
            val done = root.findAccessibilityNodeInfosByText("App installed").isNotEmpty()
                || root.findAccessibilityNodeInfosByViewId("com.android.packageinstaller:id/install_success_text")
                    .any { it.isVisibleToUser }
            if (done) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
        }
        // L4: Unknown sources / APK sideload in Settings
        // FIX: আগে `!it.isChecked` ছিল (OFF state block) — এটা ভুল ছিল।
        // Settings এ "Allow from this source" screen দেখলেই block করো — toggle state যাই হোক।
        if (pkg == "com.android.settings") {
            val unkScreen = root.findAccessibilityNodeInfosByText("Allow from this source").any { it.isVisibleToUser }
                || root.findAccessibilityNodeInfosByText("Install unknown apps").any { it.isVisibleToUser }
            if (unkScreen) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
        }
        // L4b: APK from file managers
        val fileMgrs = setOf(
            "com.estrongs.android.pop", "com.google.android.apps.nbu.files",
            "com.sec.android.app.myfiles", "com.mi.android.globalFileexplorer",
            "com.asus.filemanager", "com.alphainventor.filemanager", "com.android.documentsui"
        )
        if (fileMgrs.contains(pkg)) {
            val apk = root.findAccessibilityNodeInfosByText(".apk").any { it.isVisibleToUser && it.isClickable }
                || root.findAccessibilityNodeInfosByText("Install").any { it.isClickable }
            if (apk) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
        }
        // L5: OEM stores
        val oemMap = mapOf(
            "com.sec.android.app.samsungapps" to "com.sec.android.app.samsungapps:id/btn_install",
            "com.xiaomi.mipicks"               to "com.xiaomi.mipicks:id/install_button",
            "com.huawei.appmarket"             to "com.huawei.appmarket:id/button_install"
        )
        for ((oemPkg, btnId) in oemMap) {
            if (pkg == oemPkg) {
                val btn = root.findAccessibilityNodeInfosByText("Install").any { it.isClickable && it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByViewId(btnId).any { it.isVisibleToUser }
                if (btn) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            }
        }
        val oemStores = setOf("com.oppo.market", "com.heytap.market", "com.vivo.appstore", "com.oneplus.store")
        if (oemStores.contains(pkg)) {
            if (root.findAccessibilityNodeInfosByText("Install").any { it.isClickable && it.isVisibleToUser }
                || root.findAccessibilityNodeInfosByText("Get").any { it.isClickable && it.isVisibleToUser }) {
                performGlobalAction(GLOBAL_ACTION_HOME); return true
            }
        }
        return false
    }

    // ── 11. Uninstall Protection — 10 Layers ─────────────────
    private fun handleUninstallProtection(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.uninstallProtection) return false
        try {
            // L1: Settings app info
            if (pkg == "com.android.settings") {
                if (root.findAccessibilityNodeInfosByViewId("com.android.settings:id/uninstall_button")
                        .any { it.isVisibleToUser }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
                val header = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/entity_header_title")
                    .any { it.text?.toString()?.contains(rasFocusName, true) == true }
                val unBtn = root.findAccessibilityNodeInfosByText("Uninstall").any { it.isClickable && it.isVisibleToUser }
                if (header && unBtn) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
                if (root.findAccessibilityNodeInfosByText("Do you want to uninstall this app?").isNotEmpty()
                    || root.findAccessibilityNodeInfosByText("Uninstall $rasFocusName?").isNotEmpty()) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            // L2: Play Store
            if (pkg == "com.android.vending") {
                if (root.findAccessibilityNodeInfosByViewId("com.android.vending:id/uninstall_button")
                        .any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Uninstall").any { it.isVisibleToUser && it.isClickable }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            // L3: Package Installer
            val instPkgs = setOf("com.android.packageinstaller", "com.google.android.packageinstaller")
            if (instPkgs.contains(pkg)) {
                if (root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Do you want to uninstall this app?").isNotEmpty()) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            // L4: Force Stop / Clear Data
            if (pkg == "com.android.settings") {
                if (root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isVisibleToUser }) {
                    if (root.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")
                            .any { it.isVisibleToUser && it.isEnabled }
                        || root.findAccessibilityNodeInfosByText("Force stop").any { it.isClickable }) {
                        performGlobalAction(GLOBAL_ACTION_HOME); return true
                    }
                    if (root.findAccessibilityNodeInfosByViewId("com.android.settings:id/clear_user_data_button")
                            .any { it.isVisibleToUser }) {
                        performGlobalAction(GLOBAL_ACTION_HOME); return true
                    }
                }
            }
            // L5: Third party file managers
            val fileMgrs5 = setOf(
                "com.estrongs.android.pop", "com.google.android.apps.nbu.files",
                "com.sec.android.app.myfiles", "com.mi.android.globalFileexplorer",
                "com.asus.filemanager", "com.alphainventor.filemanager"
            )
            if (fileMgrs5.contains(pkg)) {
                if (root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isVisibleToUser }
                    && root.findAccessibilityNodeInfosByText("Uninstall").any { it.isClickable }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            // L6: Home screen long press
            // FIX: "Remove" text false positive (widget remove, icon remove)।
            // "Remove" শুধু block করো যখন "Uninstall" dialog/text-ও দেখা যাচ্ছে — তাহলে নিশ্চিত app remove।
            val launchers = setOf(
                "com.google.android.apps.nexuslauncher", "com.samsung.android.launcher",
                "com.miui.home", "com.huawei.android.launcher", "com.oppo.launcher",
                "com.vivo.launcher", "com.android.launcher", "com.android.launcher3",
                "com.teslacoilsw.launcher", "org.zimmob.zimlx"
            )
            if (launchers.contains(pkg)) {
                if (root.findAccessibilityNodeInfosByText("Uninstall").any { it.isVisibleToUser }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
                // "Remove" শুধু block যদি app name-ও দেখা যায় — widget remove false positive এড়াতে
                val hasAppName = root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isVisibleToUser }
                val hasRemove  = root.findAccessibilityNodeInfosByText("Remove").any { it.isVisibleToUser && it.isClickable }
                if (hasAppName && hasRemove) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            }
            // L7: Game launchers
            val gameLaunchers = setOf(
                "com.samsung.android.game.gamehome", "com.garena.game.freefire",
                "com.mobile.legends", "com.tencent.ig", "com.dts.freefireth"
            )
            if (gameLaunchers.contains(pkg)) {
                val hasApp = root.findAccessibilityNodeInfosByText(rasFocusName)
                    .any { it.isChecked || it.isSelected || it.isVisibleToUser }
                val hasDel = root.findAccessibilityNodeInfosByText("Uninstall").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Delete").any { it.isVisibleToUser }
                if (hasApp && hasDel) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            }
            // L8: Device Admin
            if (pkg == "com.android.settings") {
                val adminScreen = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/device_admin_settings").isNotEmpty()
                    || root.findAccessibilityNodeInfosByText("Device admin apps").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Device Administrator").any { it.isVisibleToUser }
                if (adminScreen && root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isClickable || it.isVisibleToUser }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
                if (root.findAccessibilityNodeInfosByText("Deactivate").any { it.isVisibleToUser && it.isClickable }
                    || root.findAccessibilityNodeInfosByText("Deactivate this device admin app").any { it.isVisibleToUser }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            // L9: Accessibility service page
            if (pkg == "com.android.settings") {
                val accScreen = root.findAccessibilityNodeInfosByText("Accessibility").any { it.isVisibleToUser }
                if (accScreen && root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isVisibleToUser }) {
                    if (root.findAccessibilityNodeInfosByViewId("com.android.settings:id/switch_widget")
                            .any { it.isVisibleToUser && it.isChecked }
                        || root.findAccessibilityNodeInfosByText("Turn off").any { it.isVisibleToUser }
                        || root.findAccessibilityNodeInfosByText("Stop").any { it.isClickable }) {
                        performGlobalAction(GLOBAL_ACTION_HOME); return true
                    }
                }
            }
            // L10: Running services
            if (pkg == "com.android.settings") {
                val running = root.findAccessibilityNodeInfosByText("Running services").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Running apps").any { it.isVisibleToUser }
                if (running && root.findAccessibilityNodeInfosByText(rasFocusName).any { it.isVisibleToUser }) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }
            // Global fallback
            val uninstallTexts = listOf(
                "Uninstall $rasFocusName?", "Remove $rasFocusName?",
                "Delete $rasFocusName?",    "Do you want to uninstall $rasFocusName?"
            )
            for (txt in uninstallTexts) {
                if (root.findAccessibilityNodeInfosByText(txt).isNotEmpty()) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }

            // L11: itel Freezer Protection (package: com.itel)
            if (pkg == "com.itel") {
                val appNodes = root.findAccessibilityNodeInfosByText(rasFocusName)
                for (node in appNodes) {
                    if (node.isChecked || node.isSelected) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        return true
                    }
                    val parent = node.parent
                    if (parent != null && (parent.isChecked || parent.isSelected)) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        return true
                    }
                }
            }

        } catch (e: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        return false
    }

    // ── 12. Reboot / Power / ADB Protection ──────────────────
    private fun handleRebootProtection(root: AccessibilityNodeInfo, pkg: String): Boolean {
        if (!prefs.blockReboot && !prefs.blockPowerOff && !prefs.blockSafeMode
            && !prefs.blockRecovery && !prefs.blockAdb) return false
        try {
            // OEM systemui packages — Samsung, Xiaomi, MIUI, Huawei, OnePlus, Oppo, Vivo সব cover
            val powerMenuPkgs = setOf(
                "com.android.systemui",
                "com.samsung.android.systemui",
                "com.miui.systemui",
                "com.huawei.systemmanager",
                "com.oppo.systemui",
                "com.vivo.systemui",
                "com.oneplus.systemui",
                "com.coloros.systemui"
            )

            // L1: Power menu — Restart/Reboot
            if (prefs.blockReboot && powerMenuPkgs.contains(pkg)) {
                val reboot = root.findAccessibilityNodeInfosByText("Restart").any { it.isVisibleToUser && it.isClickable }
                    || root.findAccessibilityNodeInfosByText("Reboot").any { it.isVisibleToUser && it.isClickable }
                    || root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/restart_button").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByViewId("com.samsung.android.systemui:id/restart_button").any { it.isVisibleToUser }
                if (reboot) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

                val pmVisible = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/global_actions_grid_item").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/power_menu").any { it.isVisibleToUser }
                if (pmVisible && (root.findAccessibilityNodeInfosByText("Restart").any { it.isVisibleToUser }
                        || root.findAccessibilityNodeInfosByText("Reboot").any { it.isVisibleToUser })) {
                    performGlobalAction(GLOBAL_ACTION_HOME); return true
                }
            }

            // L2: Power off
            if (prefs.blockPowerOff && powerMenuPkgs.contains(pkg)) {
                val powerOff = root.findAccessibilityNodeInfosByText("Power off").any { it.isVisibleToUser && it.isClickable }
                    || root.findAccessibilityNodeInfosByText("Shut down").any { it.isVisibleToUser && it.isClickable }
                    || root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/power_off_button").any { it.isVisibleToUser }
                if (powerOff) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
                // Power off confirm dialog
                val confirm = root.findAccessibilityNodeInfosByText("Power off?").isNotEmpty()
                    || root.findAccessibilityNodeInfosByText("Shut down?").isNotEmpty()
                if (confirm) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            }

            // L3: Safe mode
            if (prefs.blockSafeMode) {
                val safeMode = root.findAccessibilityNodeInfosByText("Safe mode").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Running in safe mode").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Restart to safe mode?").isNotEmpty()
                    || root.findAccessibilityNodeInfosByText("Reboot into safe mode?").isNotEmpty()
                    || root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/safe_mode_text").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByViewId("com.samsung.android.systemui:id/safe_mode_button").any { it.isVisibleToUser }
                if (safeMode) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            }

            // L4: Recovery / Bootloader / Factory reset
            if (prefs.blockRecovery) {
                val recovery = root.findAccessibilityNodeInfosByText("Recovery mode").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Reboot to recovery").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Unlock bootloader").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Fastboot mode").any { it.isVisibleToUser }
                if (recovery) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

                if (pkg == "com.android.settings") {
                    val factory = root.findAccessibilityNodeInfosByText("Factory data reset").any { it.isVisibleToUser }
                        || root.findAccessibilityNodeInfosByText("Erase all data").any { it.isVisibleToUser }
                        || root.findAccessibilityNodeInfosByText("Reset phone").any { it.isVisibleToUser }
                        || root.findAccessibilityNodeInfosByViewId("com.android.settings:id/eraseButton").any { it.isVisibleToUser }
                    if (factory) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
                }
            }

            // L5: ADB
            // FIX: আগে switch OFF থাকলে block করছিল — এটা ভুল ছিল।
            // ADB Settings screen দেখলেই সরাসরি HOME পাঠাও — toggle state check দরকার নেই।
            // কারণ: screen-এ ঢোকা মানেই enable করার attempt।
            if (prefs.blockAdb && pkg == "com.android.settings") {
                val adbDialog = root.findAccessibilityNodeInfosByText("Allow USB debugging?").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Allow wireless debugging?").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("RSA key fingerprint").any { it.isVisibleToUser }
                if (adbDialog) { performGlobalAction(GLOBAL_ACTION_HOME); return true }

                // ADB screen দেখলেই block — toggle state নির্বিশেষে
                val adbScreen = root.findAccessibilityNodeInfosByText("USB debugging").any { it.isVisibleToUser }
                    || root.findAccessibilityNodeInfosByText("Wireless debugging").any { it.isVisibleToUser }
                if (adbScreen) { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            }
        } catch (e: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME); return true
        }
        return false
    }
}


// ════════════════════════════════════════════════════════════
// D. SETTINGS UI — Jetpack Compose
// Design: Dark industrial + neon accent — unique, unforgettable
// ════════════════════════════════════════════════════════════

class RasFocusSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RasFocusSettingsTheme {
                SettingsScreen()
            }
        }
    }
}

// ── Theme ─────────────────────────────────────────────────

private val BG_DEEP       = Color(0xFFF4FBFD)   // very light teal-white background
private val BG_CARD       = Color(0xFFFFFFFF)   // pure white cards
private val BG_CARD2      = Color(0xFFEBF7FA)   // teal-tinted secondary card
private val ACCENT        = Color(0xFF0096B4)   // teal (RasFocus brand)
private val ACCENT2       = Color(0xFF7B5CFA)   // electric violet
private val ACCENT_RED    = Color(0xFFE53935)   // danger red
private val ACCENT_AMBER  = Color(0xFFF57C00)   // warning amber
private val TEXT_PRIMARY  = Color(0xFF0A1628)   // near-black
private val TEXT_SEC      = Color(0xFF6B7F8E)   // muted grey
private val DIVIDER       = Color(0xFFCDE8F0)   // teal-tinted divider
private val SWITCH_ON     = ACCENT
private val SWITCH_OFF    = Color(0xFFD6E8EE)

@Composable
fun RasFocusSettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = BG_DEEP,
            surface = BG_CARD,
            primary = ACCENT,
            onBackground = TEXT_PRIMARY,
            onSurface = TEXT_PRIMARY
        ),
        content = content
    )
}

// ── Main Screen ───────────────────────────────────────────

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { BlockerPrefs(ctx) }

    // State holders
    var blockAdult       by remember { mutableStateOf(prefs.blockAdult) }
    var blockAdultSiteList by remember { mutableStateOf(prefs.blockAdultSiteList) }
    var blockSearch      by remember { mutableStateOf(prefs.blockSearch) }
    var blockReels       by remember { mutableStateOf(prefs.blockReels) }
    var blockInstaSearch by remember { mutableStateOf(prefs.blockInstaSearch) }
    var blockYtShorts    by remember { mutableStateOf(prefs.blockYtShorts) }
    var blockWaChannels  by remember { mutableStateOf(prefs.blockWaChannels) }

    // ── নতুন Block Reels/Shorts per-app state ──
    var blockInstaStories      by remember { mutableStateOf(prefs.blockInstaStories) }
    var blockWaStatus          by remember { mutableStateOf(prefs.blockWaStatus) }
    var blockWaBusinessStatus  by remember { mutableStateOf(prefs.blockWaBusinessStatus) }
    var blockWaBusinessChannels by remember { mutableStateOf(prefs.blockWaBusinessChannels) }
    var blockSnapSpotlight     by remember { mutableStateOf(prefs.blockSnapSpotlight) }
    var blockSnapStories       by remember { mutableStateOf(prefs.blockSnapStories) }
    var blockTikTok            by remember { mutableStateOf(prefs.blockTikTok) }
    var blockTikTokLive        by remember { mutableStateOf(prefs.blockTikTokLive) }

    var blockUnsupported by remember { mutableStateOf(prefs.blockUnsupported) }
    var blockNewApps     by remember { mutableStateOf(prefs.blockNewApps) }
    var blockFbVideo     by remember { mutableStateOf(prefs.blockFbVideo) }

    var uninstallProt    by remember { mutableStateOf(prefs.uninstallProtection) }
    var blockAdb         by remember { mutableStateOf(prefs.blockAdb) }
    var blockPowerOff    by remember { mutableStateOf(prefs.blockPowerOff) }
    var blockSafeMode    by remember { mutableStateOf(prefs.blockSafeMode) }
    var blockReboot      by remember { mutableStateOf(prefs.blockReboot) }
    var blockRecovery    by remember { mutableStateOf(prefs.blockRecovery) }

    var blockedMsg       by remember { mutableStateOf(prefs.blockedMessage) }
    var redirectUrl      by remember { mutableStateOf(prefs.redirectUrl) }

    // ── Focus Lock state ──
    var focusLockActive   by remember { mutableStateOf(prefs.focusLockActive) }
    var focusLockMode     by remember { mutableStateOf(prefs.focusLockMode) }
    var focusLockEndTime  by remember { mutableStateOf(prefs.focusLockEndTime) }
    var showFocusSetupDialog  by remember { mutableStateOf(false) }
    var showFocusUnlockDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_DEEP)
    ) {
        // Subtle teal grid on background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 40.dp.toPx()
            val cols = (size.width / spacing).toInt() + 1
            val rows = (size.height / spacing).toInt() + 1
            for (col in 0..cols) {
                drawLine(Color(0xFFD0EBF4), Offset(col * spacing, 0f), Offset(col * spacing, size.height), strokeWidth = 0.6f)
            }
            for (row in 0..rows) {
                drawLine(Color(0xFFD0EBF4), Offset(0f, row * spacing), Offset(size.width, row * spacing), strokeWidth = 0.6f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 88.dp) // space for fixed focus lock bar
        ) {
            // ── Header ──
            Spacer(Modifier.height(8.dp))
            HeaderBar()
            Spacer(Modifier.height(20.dp))

            // ── Content Blocking ──
            SectionHeader(
                icon = "⊡",
                title = "Content Blocking",
                accentColor = ACCENT
            )
            Spacer(Modifier.height(8.dp))
            BlockingCard {
                RasSwitch(
                    label = "Block adult content",
                    sublabel = "Blocks adult URLs in Chrome",
                    checked = blockAdult,
                    accentColor = ACCENT_RED,
                    onCheckedChange = {
                        if (it || !focusLockActive) {
                            blockAdult = it
                            prefs.blockAdult = it
                            // Switch ON হলে already open সাইটও block
                            if (it) RasFocusBlockingService.instance?.checkCurrentWindow()
                        }
                    }
                )
                RasDivider()
                // ── Adult Site List (adultsite.txt) ──────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Block adult sites (list)",
                            color = TEXT_PRIMARY,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Blocks domains from adultsite.txt list",
                            color = TEXT_SEC,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // Toggle button styled like the app theme
                    Box(
                        modifier = Modifier
                            .background(
                                if (blockAdultSiteList) ACCENT_RED.copy(alpha = 0.18f)
                                else Color(0xFFE8EDF3),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (blockAdultSiteList) ACCENT_RED.copy(alpha = 0.6f)
                                else Color(0xFFDDE3EB),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                // focus active থাকলে শুধু ON করা যাবে, OFF করা যাবে না
                                val newValue = !blockAdultSiteList
                                if (newValue || !focusLockActive) {
                                    blockAdultSiteList = newValue
                                    prefs.blockAdultSiteList = newValue
                                    // Switch ON হলে already open সাইটও block
                                    if (newValue) RasFocusBlockingService.instance?.checkCurrentWindow()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (blockAdultSiteList) "ON" else "OFF",
                            color = if (blockAdultSiteList) ACCENT_RED else TEXT_SEC,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                RasDivider()
                RasSwitch(
                    label = "Block image & video search",
                    sublabel = "Blocks Google image/video tab",
                    checked = blockSearch,
                    accentColor = ACCENT,
                    onCheckedChange = { if (it || !focusLockActive) { blockSearch = it; prefs.blockSearch = it } }
                )
                RasDivider()
                RasSwitch(
                    label = "Block Instagram/Facebook Reels",
                    sublabel = "Hides Reels tab on both apps",
                    checked = blockReels,
                    accentColor = ACCENT,
                    onCheckedChange = { if (it || !focusLockActive) { blockReels = it; prefs.blockReels = it } }
                )
                RasDivider()
                RasSwitch(
                    label = "Block Instagram search",
                    sublabel = "Blocks Explore & search tab",
                    checked = blockInstaSearch,
                    accentColor = ACCENT,
                    onCheckedChange = { if (it || !focusLockActive) { blockInstaSearch = it; prefs.blockInstaSearch = it } }
                )
                RasDivider()
                RasSwitch(
                    label = "Block YouTube Shorts",
                    sublabel = "Blocks Shorts tab & player",
                    checked = blockYtShorts,
                    accentColor = ACCENT,
                    onCheckedChange = { if (it || !focusLockActive) { blockYtShorts = it; prefs.blockYtShorts = it } }
                )
                RasDivider()
                RasSwitch(
                    label = "Block WhatsApp channels",
                    sublabel = "Blocks Updates/Channels tab",
                    checked = blockWaChannels,
                    accentColor = ACCENT,
                    onCheckedChange = { if (it || !focusLockActive) { blockWaChannels = it; prefs.blockWaChannels = it } }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Block Reels / Shorts — per-app breakdown ──
            SectionHeader(
                icon = "▶",
                title = "Block Reels / Shorts",
                accentColor = ACCENT
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Block distracting content like Shorts, Reels, and other in-app feeds to stay focused.",
                color = TEXT_SEC,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BG_CARD, RoundedCornerShape(10.dp))
                    .border(1.dp, DIVIDER, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            Spacer(Modifier.height(8.dp))

            // YouTube
            AppReelsCard(
                appIcon = "▶",
                appName = "YouTube",
                appIconColor = Color(0xFFFF0000),
                rows = listOf(
                    AppReelsRow.Toggle(
                        label = "Shorts",
                        checked = blockYtShorts,
                        onCheckedChange = { if (it || !focusLockActive) { blockYtShorts = it; prefs.blockYtShorts = it } }
                    )
                )
            )
            Spacer(Modifier.height(8.dp))

            // Instagram
            AppReelsCard(
                appIcon = "📷",
                appName = "Instagram",
                appIconColor = Color(0xFFE1306C),
                rows = listOf(
                    AppReelsRow.Toggle(
                        label = "Reels",
                        checked = blockReels,
                        onCheckedChange = { if (it || !focusLockActive) { blockReels = it; prefs.blockReels = it } }
                    ),
                    AppReelsRow.Upgrade(label = "Stories")
                )
            )
            Spacer(Modifier.height(8.dp))

            // WhatsApp
            AppReelsCard(
                appIcon = "💬",
                appName = "WhatsApp",
                appIconColor = Color(0xFF25D366),
                rows = listOf(
                    AppReelsRow.Upgrade(label = "Status"),
                    AppReelsRow.Upgrade(label = "Channels")
                )
            )
            Spacer(Modifier.height(8.dp))

            // Snapchat
            AppReelsCard(
                appIcon = "👻",
                appName = "Snapchat",
                appIconColor = Color(0xFFFFFC00),
                rows = listOf(
                    AppReelsRow.Toggle(
                        label = "Spotlight",
                        checked = blockSnapSpotlight,
                        onCheckedChange = { if (it || !focusLockActive) { blockSnapSpotlight = it; prefs.blockSnapSpotlight = it } }
                    ),
                    AppReelsRow.Upgrade(label = "Stories")
                )
            )
            Spacer(Modifier.height(8.dp))

            // WA Business
            AppReelsCard(
                appIcon = "💼",
                appName = "WA Business",
                appIconColor = Color(0xFF128C7E),
                rows = listOf(
                    AppReelsRow.Upgrade(label = "Status"),
                    AppReelsRow.Upgrade(label = "Channels")
                )
            )
            Spacer(Modifier.height(8.dp))

            // TikTok
            AppReelsCard(
                appIcon = "🎵",
                appName = "TikTok",
                appIconColor = Color(0xFF010101),
                rows = listOf(
                    AppReelsRow.Toggle(
                        label = "Feed / For You",
                        checked = blockTikTok,
                        onCheckedChange = { if (it || !focusLockActive) { blockTikTok = it; prefs.blockTikTok = it } }
                    ),
                    AppReelsRow.Upgrade(label = "LIVE")
                )
            )
            SectionHeader(icon = "◈", title = "Advanced Blocking", accentColor = ACCENT2)
            Spacer(Modifier.height(8.dp))
            BlockingCard {
                RasSwitch(
                    label = "Block unsupported browsers",
                    sublabel = "Opera, Firefox, Brave, UC & more",
                    checked = blockUnsupported,
                    accentColor = ACCENT2,
                    onCheckedChange = { if (it || !focusLockActive) { blockUnsupported = it; prefs.blockUnsupported = it } }
                )
                RasDivider()
                RasSwitch(
                    label = "Block newly installed apps",
                    sublabel = "Prevents all new installs & APK sideloads",
                    checked = blockNewApps,
                    accentColor = ACCENT2,
                    onCheckedChange = { if (it || !focusLockActive) { blockNewApps = it; prefs.blockNewApps = it } }
                )
                RasDivider()
                RasSwitch(
                    label = "Block Facebook video",
                    sublabel = "Blocks Watch, Reels & inline videos",
                    checked = blockFbVideo,
                    accentColor = ACCENT2,
                    onCheckedChange = { if (it || !focusLockActive) { blockFbVideo = it; prefs.blockFbVideo = it } }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Protection (Uninstall + Power) ──
            SectionHeader(icon = "⬡", title = "App Protection", accentColor = ACCENT_RED)
            Spacer(Modifier.height(8.dp))

            // Uninstall master toggle — highlighted
            MasterToggleCard(
                label = "Uninstall protection",
                sublabel = "10-layer block — Settings, Play Store, ADB, launchers & more",
                checked = uninstallProt,
                onCheckedChange = { if (it || !focusLockActive) { uninstallProt = it; prefs.uninstallProtection = it } }
            )

            Spacer(Modifier.height(8.dp))

            // Power / System protection sub-switches
            BlockingCard {
                SubSwitchRow(
                    icon = "⏻",
                    label = "Block power off",
                    sublabel = "Prevents device shutdown",
                    checked = blockPowerOff,
                    accentColor = ACCENT_AMBER,
                    onCheckedChange = { if (it || !focusLockActive) { blockPowerOff = it; prefs.blockPowerOff = it } }
                )
                RasDivider()
                SubSwitchRow(
                    icon = "↺",
                    label = "Block reboot",
                    sublabel = "Blocks restart from power menu & Settings",
                    checked = blockReboot,
                    accentColor = ACCENT_AMBER,
                    onCheckedChange = { if (it || !focusLockActive) { blockReboot = it; prefs.blockReboot = it } }
                )
                RasDivider()
                SubSwitchRow(
                    icon = "⚠",
                    label = "Block safe mode",
                    sublabel = "Detects & exits safe mode boot",
                    checked = blockSafeMode,
                    accentColor = ACCENT_AMBER,
                    onCheckedChange = { if (it || !focusLockActive) { blockSafeMode = it; prefs.blockSafeMode = it } }
                )
                RasDivider()
                SubSwitchRow(
                    icon = "⟳",
                    label = "Block recovery / factory reset",
                    sublabel = "Blocks recovery mode, bootloader, factory reset",
                    checked = blockRecovery,
                    accentColor = ACCENT_RED,
                    onCheckedChange = { if (it || !focusLockActive) { blockRecovery = it; prefs.blockRecovery = it } }
                )
                RasDivider()
                SubSwitchRow(
                    icon = "⌁",
                    label = "Block ADB / USB debugging",
                    sublabel = "Blocks ADB authorization dialogs",
                    checked = blockAdb,
                    accentColor = ACCENT_RED,
                    onCheckedChange = { if (it || !focusLockActive) { blockAdb = it; prefs.blockAdb = it } }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Customize Blocked Screen ──
            SectionHeader(icon = "✦", title = "Customize Blocked Screen", accentColor = TEXT_SEC)
            Spacer(Modifier.height(8.dp))
            BlockingCard {
                CustomizeRow(
                    label = "Blocked screen message",
                    value = blockedMsg,
                    placeholder = "This page is blocked.",
                    onValueChange = { blockedMsg = it; prefs.blockedMessage = it }
                )
                RasDivider()
                CustomizeRow(
                    label = "Redirect after closing (any URL)",
                    value = redirectUrl,
                    placeholder = "https://www.google.com",
                    onValueChange = { redirectUrl = it; prefs.redirectUrl = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(32.dp))
        }

        // ── Fixed Focus Lock bar pinned to top ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            FocusLockTopBar(
                isActive = focusLockActive,
                endTime = focusLockEndTime,
                onStartClick = { showFocusSetupDialog = true },
                onUnlockClick = { showFocusUnlockDialog = true }
            )
        }

        // ── Setup Dialog ──
        if (showFocusSetupDialog) {
            FocusLockSetupDialog(
                prefs = prefs,
                onDismiss = { showFocusSetupDialog = false },
                onActivated = { mode, endMs ->
                    focusLockActive = true
                    focusLockMode = mode
                    focusLockEndTime = endMs
                    prefs.focusLockActive = true
                    prefs.focusLockMode = mode
                    prefs.focusLockEndTime = endMs
                    showFocusSetupDialog = false
                }
            )
        }

        // ── Unlock Dialog ──
        if (showFocusUnlockDialog) {
            FocusLockUnlockDialog(
                prefs = prefs,
                onDismiss = { showFocusUnlockDialog = false },
                onUnlocked = {
                    focusLockActive = false
                    focusLockMode = "none"
                    focusLockEndTime = 0L
                    prefs.focusLockActive = false
                    prefs.focusLockMode = "none"
                    prefs.focusLockEndTime = 0L
                    showFocusUnlockDialog = false
                }
            )
        }
    }
}

// ── UI Components ─────────────────────────────────────────

@Composable
fun HeaderBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(ACCENT.copy(alpha = 0.08f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, ACCENT.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Logo mark
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradientBrush(
                            listOf(ACCENT, ACCENT2),
                            start = Offset(0f, 0f),
                            end = Offset(44f, 44f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("RF", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "RasFocus",
                    color = ACCENT,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Content Blocker",
                    color = TEXT_SEC,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(ACCENT.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, ACCENT.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("● ACTIVE", color = ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(icon: String, title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = accentColor, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    title.uppercase(),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun BlockingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG_CARD, RoundedCornerShape(16.dp))
            .border(1.5.dp, ACCENT.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = ACCENT.copy(alpha = 0.08f))
    ) {
        // Teal top strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(ACCENT, ACCENT.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        )
        content()
    }
}

@Composable
fun MasterToggleCard(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val glowAlpha by animateFloatAsState(if (checked) 0.25f else 0f, tween(400))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = if (checked)
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(ACCENT_RED.copy(alpha = 0.08f), Color(0xFFFFF5F5))
                    )
                else
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(ACCENT.copy(alpha = 0.07f), Color(0xFFF0FAFC))
                    ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                1.5.dp,
                if (checked) ACCENT_RED.copy(alpha = 0.45f) else ACCENT.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Teal/red icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (checked) ACCENT_RED.copy(alpha = 0.12f) else ACCENT.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (checked) "🛡" else "🔓", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TEXT_PRIMARY, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(sublabel, color = TEXT_SEC, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(12.dp))
            RasToggle(checked = checked, accentColor = ACCENT_RED, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun RasSwitch(
    label: String,
    sublabel: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val bgColor by animateColorAsState(
        if (checked) accentColor.copy(alpha = 0.08f) else BG_CARD, tween(200)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Teal left dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (checked) accentColor else DIVIDER,
                    CircleShape
                )
                .then(
                    if (checked) Modifier.shadow(4.dp, CircleShape, spotColor = accentColor)
                    else Modifier
                )
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = if (checked) accentColor else TEXT_PRIMARY, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(sublabel, color = TEXT_SEC, fontSize = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        RasToggle(checked = checked, accentColor = accentColor, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SubSwitchRow(
    icon: String,
    label: String,
    sublabel: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val bgColor by animateColorAsState(
        if (checked) accentColor.copy(alpha = 0.05f) else Color.White, tween(200)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 14.sp, color = accentColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TEXT_PRIMARY, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(sublabel, color = TEXT_SEC, fontSize = 11.sp)
        }
        Spacer(Modifier.width(10.dp))
        RasToggle(checked = checked, accentColor = accentColor, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CustomizeRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, color = ACCENT, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TEXT_PRIMARY,
                fontSize = 13.sp
            ),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BG_DEEP, RoundedCornerShape(10.dp))
                        .border(1.5.dp, ACCENT.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (value.isEmpty()) Text(placeholder, color = TEXT_SEC, fontSize = 13.sp)
                    inner()
                }
            }
        )
    }
}

@Composable
fun RasToggle(checked: Boolean, accentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    val trackColor by animateColorAsState(if (checked) accentColor else Color(0xFFE2EAF0), tween(250))
    val thumbColor by animateColorAsState(if (checked) Color.White else Color(0xFFB0BEC5), tween(250))
    val offset by animateDpAsState(if (checked) 20.dp else 2.dp, tween(250))

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .background(trackColor, RoundedCornerShape(13.dp))
            .border(1.dp, if (checked) accentColor else Color(0xFFCDD5DE), RoundedCornerShape(13.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .padding(start = offset)
                .align(Alignment.CenterStart)
                .size(22.dp)
                .shadow(if (checked) 3.dp else 1.dp, CircleShape, spotColor = accentColor)
                .background(thumbColor, CircleShape)
        )
    }
}

// ── App Reels / Shorts Card ───────────────────────────────

sealed class AppReelsRow {
    data class Toggle(
        val label: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : AppReelsRow()
    data class Upgrade(val label: String) : AppReelsRow()
}

@Composable
fun AppReelsCard(
    appIcon: String,
    appName: String,
    appIconColor: Color,
    rows: List<AppReelsRow>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG_CARD, RoundedCornerShape(16.dp))
            .border(1.5.dp, ACCENT.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = ACCENT.copy(alpha = 0.06f))
    ) {
        // App header row — teal gradient strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(ACCENT.copy(alpha = 0.15f), BG_CARD2, BG_CARD)
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(appIconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, appIconColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appIcon, fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(appName, color = TEXT_PRIMARY, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(ACCENT.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .border(1.dp, ACCENT.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("${rows.size} rules", color = ACCENT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Teal divider
        Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(ACCENT.copy(alpha = 0.4f), ACCENT.copy(alpha = 0.1f), Color.Transparent)
            )
        ))

        // Rows
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (index % 2 == 0) BG_CARD else BG_DEEP
                    )
                    .then(
                        if (index == rows.lastIndex)
                            Modifier.background(
                                if (index % 2 == 0) BG_CARD else BG_DEEP,
                                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            )
                        else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Teal dot indicator
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(ACCENT.copy(alpha = 0.45f), CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    when (row) {
                        is AppReelsRow.Toggle -> row.label
                        is AppReelsRow.Upgrade -> row.label
                    },
                    color = TEXT_PRIMARY,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                when (row) {
                    is AppReelsRow.Toggle -> {
                        RasToggle(
                            checked = row.checked,
                            accentColor = ACCENT,
                            onCheckedChange = row.onCheckedChange
                        )
                    }
                    is AppReelsRow.Upgrade -> {
                        UpgradeButton()
                    }
                }
            }
            if (index < rows.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 14.dp).background(DIVIDER))
            }
        }
    }
}

@Composable
fun UpgradeButton() {
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFFF8C00), Color(0xFFFFB800))),
                RoundedCornerShape(20.dp)
            )
            .clickable { /* TODO: navigate to upgrade screen */ }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👑", fontSize = 11.sp)
            Spacer(Modifier.width(5.dp))
            Text(
                "Upgrade",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun RasDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(ACCENT.copy(alpha = 0.08f), DIVIDER, ACCENT.copy(alpha = 0.08f))
                )
            )
    )
}

@Composable
fun FocusModeButton() {
    var active by remember { mutableStateOf(false) }
    val pulse by rememberInfiniteTransition().animateFloat(
        0.85f, 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradientBrush(
                        if (active) listOf(ACCENT, ACCENT2) else listOf(Color(0xFFECF4F8), Color(0xFFECF4F8)),
                        start = Offset(0f, 0f),
                        end = Offset(200f, 60f)
                    ),
                    RoundedCornerShape(50.dp)
                )
                .border(
                    1.dp,
                    if (active) ACCENT.copy(0.5f) else Color(0xFFD0D8E4),
                    RoundedCornerShape(50.dp)
                )
                .clickable { active = !active }
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .then(if (active) Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse } else Modifier)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (active) "◉" else "◎", color = if (active) Color.Black else ACCENT, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (active) "Focus Mode ON" else "Focus Mode",
                    color = if (active) Color.Black else TEXT_PRIMARY,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// FOCUS LOCK — Top Bar, Setup Dialog, Unlock Dialog
// ══════════════════════════════════════════════════════════════════

// ── Focus Lock Mode Item ──────────────────────────────────────────
private data class ModeItem(val code: String, val icon: String, val title: String, val sub: String)

// ── Strings (Bilingual) ───────────────────────────────────────────
private object FL {
    fun str(lang: String, bn: String, en: String) = if (lang == "bn") bn else en
}

// ── Fixed Top Bar ─────────────────────────────────────────────────
@Composable
fun FocusLockTopBar(
    isActive: Boolean,
    endTime: Long,
    onStartClick: () -> Unit,
    onUnlockClick: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.96f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // Countdown timer display
    var remainingMs by remember { mutableStateOf(0L) }
    LaunchedEffect(isActive, endTime) {
        while (isActive && endTime > 0) {
            remainingMs = maxOf(0L, endTime - System.currentTimeMillis())
            if (remainingMs == 0L) break
            kotlinx.coroutines.delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isActive)
                        Brush.horizontalGradient(listOf(ACCENT.copy(0.18f), ACCENT.copy(0.06f), BG_CARD))
                    else
                        Brush.horizontalGradient(listOf(BG_CARD, BG_DEEP)),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.5.dp,
                    if (isActive) ACCENT.copy(0.6f) else ACCENT.copy(0.2f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isActive) ACCENT.copy(0.15f) else ACCENT.copy(0.1f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isActive) "🔒" else "🎯",
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isActive) "Focus Lock Active"
                        else "Start Focus",
                        color = if (isActive) ACCENT else TEXT_PRIMARY,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActive && endTime > 0 && remainingMs > 0) {
                        val h = TimeUnit.MILLISECONDS.toHours(remainingMs)
                        val m = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
                        val s = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
                        Text(
                            "%02d:%02d:%02d".format(h, m, s),
                            color = ACCENT.copy(0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isActive) {
                        Text(
                            "Active",
                            color = ACCENT.copy(0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Action button
                Box(
                    modifier = Modifier
                        .graphicsLayer { if (isActive) { scaleX = pulse; scaleY = pulse } }
                        .background(
                            if (isActive)
                                Brush.horizontalGradient(listOf(Color(0xFFFF3B5C), Color(0xFFFF6B35)))
                            else
                                Brush.horizontalGradient(listOf(ACCENT, ACCENT2)),
                            RoundedCornerShape(50.dp)
                        )
                        .clickable { if (isActive) onUnlockClick() else onStartClick() }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isActive) "Unlock"
                        else "Start",
                        color = if (isActive) Color.White else Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Setup Dialog ─────────────────────────────────────────────────
@Composable
fun FocusLockSetupDialog(
    prefs: BlockerPrefs,
    onDismiss: () -> Unit,
    onActivated: (mode: String, endMs: Long) -> Unit
) {
    // Step 0: mode → Step 1: config
    var step by remember { mutableStateOf(0) }  // 0=mode, 1=config
    val lang = "en"
    var selectedMode by remember { mutableStateOf("") }

    // Self mode fields
    var selfDays    by remember { mutableStateOf(0) }
    var selfHours   by remember { mutableStateOf(0) }
    var selfMinutes by remember { mutableStateOf(25) }

    // Parents mode
    var parentPass  by remember { mutableStateOf("") }
    var parentPass2 by remember { mutableStateOf("") }
    var passError   by remember { mutableStateOf("") }

    // Long text mode
    val longTextPassage = "Read carefully and type: Time is the most precious resource in our lives. Every moment that passes never returns. The person who respects their time moves forward in life. Distraction is our greatest enemy. Focus is power, discipline is freedom. Stay committed to your goals and make progress every day. Success does not come overnight; it is the fruit of patience and perseverance."
    var longTextInput by remember { mutableStateOf("") }
    val longTextWordCount = longTextInput.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .background(BG_CARD, RoundedCornerShape(20.dp))
                .border(1.5.dp, ACCENT.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                // Dialog title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when(step) { 0 -> "Select Mode"; else -> "Configure" },
                        color = ACCENT,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text("✕", color = TEXT_SEC, fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Step 0: Mode selection ──
                if (step == 0) {
                    val modes = listOf(
                        ModeItem("self",     "⏱",  "Self Mode",     "Set day / hour / minute"),
                        ModeItem("parents",  "🔐", "Parents Mode",  "Password protected lock"),
                        ModeItem("longtext", "📝", "Long Text Mode", "Unlock by typing 100 words")
                    )
                    modes.forEach { modeItem ->
                    val code = modeItem.code; val icon = modeItem.icon; val title = modeItem.title; val sub = modeItem.sub
                        val sel = selectedMode == code
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .background(
                                    if (sel) ACCENT.copy(0.1f) else Color(0xFFF5F7FA),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.5.dp,
                                    if (sel) ACCENT else Color(0xFFD0D8E4),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedMode = code }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = icon, fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(text = title, color = if (sel) ACCENT else TEXT_PRIMARY, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = sub, color = TEXT_SEC, fontSize = 11.sp)
                                }
                                if (sel) {
                                    Spacer(Modifier.weight(1f))
                                    Text(text = "✓", color = ACCENT, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusDialogButton("Cancel", true, modifier = Modifier.weight(1f)) { onDismiss() }
                        FocusDialogButton("Next →", selectedMode.isNotEmpty(), modifier = Modifier.weight(1f)) { step = 1 }
                    }
                }

                // ── Step 1: Config per mode ──
                if (step == 1) {
                    when (selectedMode) {

                        // SELF MODE
                        "self" -> {
                            Text(
                                "Set your focus duration:",
                                color = TEXT_SEC, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FocusNumberPicker(
                                    label = "Day",
                                    value = selfDays, min = 0, max = 30,
                                    onValueChange = { selfDays = it },
                                    modifier = Modifier.weight(1f)
                                )
                                FocusNumberPicker(
                                    label = "Hour",
                                    value = selfHours, min = 0, max = 23,
                                    onValueChange = { selfHours = it },
                                    modifier = Modifier.weight(1f)
                                )
                                FocusNumberPicker(
                                    label = "Min",
                                    value = selfMinutes, min = 0, max = 59,
                                    onValueChange = { selfMinutes = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            val totalMs = (selfDays * 86400L + selfHours * 3600L + selfMinutes * 60L) * 1000L
                            val valid = totalMs > 0
                            if (!valid) {
                                Text("Set at least 1 minute", color = ACCENT_RED, fontSize = 11.sp)
                                Spacer(Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FocusDialogButton("← Back", true, modifier = Modifier.weight(1f)) { step = 0 }
                                FocusDialogButton("🔒 Lock", valid, modifier = Modifier.weight(1f)) {
                                    onActivated("self", System.currentTimeMillis() + totalMs)
                                }
                            }
                        }

                        // PARENTS MODE
                        "parents" -> {
                            Text("Set a new password:", color = TEXT_SEC, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            FocusPasswordField(
                                value = parentPass,
                                placeholder = "Password",
                                onValueChange = { parentPass = it; passError = "" }
                            )
                            Spacer(Modifier.height(8.dp))
                            FocusPasswordField(
                                value = parentPass2,
                                placeholder = "Confirm password",
                                onValueChange = { parentPass2 = it; passError = "" }
                            )
                            if (passError.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(passError, color = ACCENT_RED, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FocusDialogButton("← Back", true, modifier = Modifier.weight(1f)) { step = 0 }
                                FocusDialogButton("🔒 Lock", parentPass.length >= 4, modifier = Modifier.weight(1f)) {
                                    when {
                                        parentPass.length < 4 -> passError = "Minimum 4 characters"
                                        parentPass != parentPass2 -> passError = "Passwords do not match"
                                        else -> {
                                            prefs.focusLockPassword = parentPass.hashCode().toString()
                                            onActivated("parents", 0L)
                                        }
                                    }
                                }
                            }
                        }

                        // LONG TEXT MODE
                        "longtext" -> {
                            Text(
                                "Remember this passage — you must type 100 words to unlock:",
                                color = TEXT_SEC, fontSize = 11.sp, lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0F8FB), RoundedCornerShape(10.dp))
                                    .border(1.dp, ACCENT.copy(0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(longTextPassage, color = ACCENT.copy(0.9f), fontSize = 11.sp, lineHeight = 17.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FocusDialogButton("← Back", true, modifier = Modifier.weight(1f)) { step = 0 }
                                FocusDialogButton("🔒 Lock", true, modifier = Modifier.weight(1f)) {
                                    prefs.focusLockLongText = longTextPassage
                                    onActivated("longtext", 0L)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Unlock Dialog ─────────────────────────────────────────────────
@Composable
fun FocusLockUnlockDialog(
    prefs: BlockerPrefs,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    val lang = "en"
    val mode = prefs.focusLockMode
    var error by remember { mutableStateOf("") }

    // Parents mode
    var passInput by remember { mutableStateOf("") }

    // Long text mode
    val requiredText = prefs.focusLockLongText
    var textInput by remember { mutableStateOf("") }
    val inputWordCount = textInput.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    val requiredWordCount = requiredText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size

    // Self mode — check if time is up
    val timeUp = mode == "self" && System.currentTimeMillis() >= prefs.focusLockEndTime

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .background(BG_CARD, RoundedCornerShape(20.dp))
                .border(1.5.dp, ACCENT.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔓", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Unlock Focus Lock",
                        color = ACCENT, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text("✕", color = TEXT_SEC, fontSize = 14.sp,
                        modifier = Modifier.clickable { onDismiss() }.padding(4.dp))
                }

                Spacer(Modifier.height(16.dp))

                when (mode) {
                    "self" -> {
                        if (timeUp) {
                            Text(
                                "✅ Time's up! You can unlock now.",
                                color = ACCENT, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            FocusDialogButton("🔓 Unlock", true) { onUnlocked() }
                        } else {
                            val remaining = maxOf(0L, prefs.focusLockEndTime - System.currentTimeMillis())
                            val h = TimeUnit.MILLISECONDS.toHours(remaining)
                            val m = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
                            val s = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                            Text(
                                "⏳ %02d:%02d:%02d remaining. Cannot unlock until time is up.".format(h, m, s),
                                color = ACCENT_AMBER, fontSize = 12.sp, lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            FocusDialogButton("OK", true) { onDismiss() }
                        }
                    }

                    "parents" -> {
                        Text("Enter password:", color = TEXT_SEC, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        FocusPasswordField(
                            value = passInput,
                            placeholder = "Password",
                            onValueChange = { passInput = it; error = "" }
                        )
                        if (error.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(error, color = ACCENT_RED, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        FocusDialogButton("🔓 Unlock", passInput.isNotEmpty()) {
                            if (passInput.hashCode().toString() == prefs.focusLockPassword) {
                                onUnlocked()
                            } else {
                                error = "❌ Wrong password!"
                            }
                        }
                    }

                    "longtext" -> {
                        Text(
                            "Type the full passage below ($requiredWordCount words):",
                            color = TEXT_SEC, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        // Show passage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0F8FB), RoundedCornerShape(10.dp))
                                .border(1.dp, ACCENT.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .heightIn(max = 120.dp)
                        ) {
                            Text(requiredText, color = TEXT_SEC, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        // Input area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFBFC), RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (inputWordCount >= requiredWordCount) ACCENT else Color(0xFFD0D8E4),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                                .heightIn(min = 80.dp, max = 140.dp)
                        ) {
                            BasicTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                textStyle = TextStyle(color = TEXT_PRIMARY, fontSize = 12.sp, lineHeight = 18.sp),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (textInput.isEmpty()) Text("Type here...", color = TEXT_SEC, fontSize = 12.sp)
                                    inner()
                                }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Text(
                                "Words: $inputWordCount / $requiredWordCount",
                                color = if (inputWordCount >= requiredWordCount) ACCENT else TEXT_SEC,
                                fontSize = 11.sp
                            )
                        }
                        if (error.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(error, color = ACCENT_RED, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        FocusDialogButton(
                            "🔓 Unlock",
                            inputWordCount >= requiredWordCount
                        ) {
                            // Simple text match check (trimmed, case-insensitive, whitespace-normalized)
                            val normalize: (String) -> String = { s ->
                                s.trim().replace("\\s+".toRegex(), " ").lowercase()
                            }
                            if (normalize(textInput) == normalize(requiredText)) {
                                onUnlocked()
                            } else {
                                error = "❌ Text doesn't match exactly!"
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared sub-components ─────────────────────────────────────────

@Composable
fun FocusDialogButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(ACCENT, ACCENT2))
                else
                    Brush.horizontalGradient(listOf(Color(0xFFDDE3EB), Color(0xFFDDE3EB))),
                RoundedCornerShape(12.dp)
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) Color.Black else TEXT_SEC,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FocusPasswordField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFBFC), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFD0D8E4), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = TEXT_PRIMARY, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = TEXT_SEC, fontSize = 14.sp)
                inner()
            }
        )
    }
}

@Composable
fun FocusNumberPicker(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp))
            .border(1.dp, DIVIDER, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TEXT_SEC, fontSize = 10.sp, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        // Up button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFECF4F8), CircleShape)
                .clickable { if (value < max) onValueChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("▲", color = ACCENT, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "%02d".format(value),
            color = TEXT_PRIMARY,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        // Down button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFECF4F8), CircleShape)
                .clickable { if (value > min) onValueChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("▼", color = ACCENT, fontSize = 10.sp)
        }
    }
}

// ── Brush helper (linearGradientBrush missing in some versions) ──
private fun Brush.Companion.linearGradientBrush(
    colors: List<Color>,
    start: Offset,
    end: Offset
): Brush = linearGradient(colors = colors, start = start, end = end)
