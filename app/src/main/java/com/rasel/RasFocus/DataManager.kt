package com.rasel.RasFocus

import android.content.Context
import android.content.SharedPreferences

/**
 * DataManager — Global singleton for all RasFocus feature states.
 * SharedPreferences-backed so data survives process restarts.
 */
object DataManager {

    private const val PREFS_NAME = "RasFocusData"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Adult Block ───────────────────────────────────────────────────────
    var isAdultFocusActive: Boolean
        get() = prefs.getBoolean("isAdultFocusActive", false)
        set(v) = prefs.edit().putBoolean("isAdultFocusActive", v).apply()

    var is24HourLockActive: Boolean
        get() = prefs.getBoolean("is24HourLockActive", false)
        set(v) = prefs.edit().putBoolean("is24HourLockActive", v).apply()

    var lock24hEndTime: Long
        get() = prefs.getLong("lock24hEndTime", 0L)
        set(v) = prefs.edit().putLong("lock24hEndTime", v).apply()

    /** 0 = Self Control, 1 = Friend Control */
    var controlMode: Int
        get() = prefs.getInt("controlMode", 0)
        set(v) = prefs.edit().putInt("controlMode", v).apply()

    /** 0 = Muslim, 1 = Hindu, 2 = Christian, 3 = Universal */
    var adultReligion: Int
        get() = prefs.getInt("adultReligion", 0)
        set(v) = prefs.edit().putInt("adultReligion", v).apply()

    /** 0 = Bangla, 1 = English */
    var adultLanguage: Int
        get() = prefs.getInt("adultLanguage", 0)
        set(v) = prefs.edit().putInt("adultLanguage", v).apply()

    var isPeriodicPopupsActive: Boolean
        get() = prefs.getBoolean("isPeriodicPopupsActive", false)
        set(v) = prefs.edit().putBoolean("isPeriodicPopupsActive", v).apply()

    var showQuotes: Boolean
        get() = prefs.getBoolean("showQuotes", true)
        set(v) = prefs.edit().putBoolean("showQuotes", v).apply()

    var totalBlockedCount: Int
        get() = prefs.getInt("totalBlockedCount", 0)
        set(v) = prefs.edit().putInt("totalBlockedCount", v).apply()

    var cleanStreakDays: Int
        get() = prefs.getInt("cleanStreakDays", 0)
        set(v) = prefs.edit().putInt("cleanStreakDays", v).apply()

    var userCustomAdultKeywords: List<String>
        get() {
            val raw = prefs.getString("userCustomAdultKeywords", "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split("|||")
        }
        set(v) = prefs.edit().putString("userCustomAdultKeywords", v.joinToString("|||")).apply()

    // ── Self-Control / Focus Block ────────────────────────────────────────
    var isFocusActive: Boolean
        get() = prefs.getBoolean("isFocusActive", false)
        set(v) = prefs.edit().putBoolean("isFocusActive", v).apply()

    /** 0 = Block list mode, 1 = Allow list mode */
    var simpleBlockMode: Int
        get() = prefs.getInt("simpleBlockMode", 0)
        set(v) = prefs.edit().putInt("simpleBlockMode", v).apply()

    var userAppList: List<String>
        get() {
            val raw = prefs.getString("userAppList", "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split("|||")
        }
        set(v) = prefs.edit().putString("userAppList", v.joinToString("|||")).apply()

    var userWebList: List<String>
        get() {
            val raw = prefs.getString("userWebList", "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split("|||")
        }
        set(v) = prefs.edit().putString("userWebList", v.joinToString("|||")).apply()

    var blockSettingsAndUninstall: Boolean
        get() = prefs.getBoolean("blockSettingsAndUninstall", false)
        set(v) = prefs.edit().putBoolean("blockSettingsAndUninstall", v).apply()

    // ── Deep Study ────────────────────────────────────────────────────────
    var isDeepStudyStrict: Boolean
        get() = prefs.getBoolean("isDeepStudyStrict", false)
        set(v) = prefs.edit().putBoolean("isDeepStudyStrict", v).apply()

    var dsAllowAppList: List<String>
        get() {
            val raw = prefs.getString("dsAllowAppList", "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split("|||")
        }
        set(v) = prefs.edit().putString("dsAllowAppList", v.joinToString("|||")).apply()

    var dsAllowWebList: List<String>
        get() {
            val raw = prefs.getString("dsAllowWebList", "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split("|||")
        }
        set(v) = prefs.edit().putString("dsAllowWebList", v.joinToString("|||")).apply()

    var dsFocusMin: Int
        get() = prefs.getInt("dsFocusMin", 25)
        set(v) = prefs.edit().putInt("dsFocusMin", v).apply()

    var dsRestMin: Int
        get() = prefs.getInt("dsRestMin", 5)
        set(v) = prefs.edit().putInt("dsRestMin", v).apply()

    var dsKeepBlockingInBreak: Boolean
        get() = prefs.getBoolean("dsKeepBlockingInBreak", false)
        set(v) = prefs.edit().putBoolean("dsKeepBlockingInBreak", v).apply()

    // ── Misc ──────────────────────────────────────────────────────────────
    fun clearAll() = prefs.edit().clear().apply()
}
