package com.rasel.RasFocus.selfcontrol

// ════════════════════════════════════════════════════════════════════════════
//  BlockerApp.kt  —  ONE FILE, COMPLETE REAL BLOCKING
//
//  এই একটা ফাইলে আছে:
//   1. Data models + BlockedData (SharedPreferences storage)
//   2. AppBlockerAccessibilityService  → foreground app detect → block screen
//   3. BlockerVpnService               → local VPN → DNS filter → site block
//   4. BlockerForegroundService        → background এ সবসময় চালু রাখে
//   5. SingleAppsBootReceiver          → device reboot হলে auto-start
//   6. BlockingActivity                → blocked app এর উপর full-screen block UI
//   7. SingleAppsActivity + Compose UI → apps tab, sites tab, block modal
// ════════════════════════════════════════════════════════════════════════════

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.*
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.io.*
import java.net.*
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.concurrent.Executors

// ════════════════════════════════════════════════════════════════════════════
//  1. DATA MODELS + SHARED STORAGE
// ════════════════════════════════════════════════════════════════════════════

enum class BlockMethod { TIME_RANGE, PASSWORD, LONG_TEXT }

data class BlockConfig(
    val method:   BlockMethod = BlockMethod.TIME_RANGE,
    val timeFrom: String      = "09:00",   // "HH:mm"
    val timeTo:   String      = "17:00",
    val password: String      = "",
    val longText: String      = ""
)

object BlockedData {

    private const val PREFS   = "blocker_prefs"
    private const val K_APPS  = "blocked_apps"
    private const val K_SITES = "blocked_sites"
    private val gson = Gson()

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Blocking Active Flag ──────────────────────────────────────────────────
    // Accessibility permission দেওয়া ≠ blocking চালু।
    // User explicitly dashboard থেকে enable করলে তবেই true হবে।
    fun isBlockingActive(ctx: Context): Boolean =
        prefs(ctx).getBoolean("is_blocking_active", false)

    fun setBlockingActive(ctx: Context, active: Boolean) {
        prefs(ctx).edit().putBoolean("is_blocking_active", active).apply()
    }

    // ── Time range check ──────────────────────────────────────────────────────
    private fun isTimeActive(cfg: BlockConfig): Boolean {
        if (cfg.method != BlockMethod.TIME_RANGE) return true
        val cal  = Calendar.getInstance()
        val now  = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val from = cfg.timeFrom.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val to   = cfg.timeTo.split(":").let   { it[0].toInt() * 60 + it[1].toInt() }
        return if (from <= to) now in from..to else now >= from || now <= to
    }

    // ── Apps ──────────────────────────────────────────────────────────────────
    fun getBlockedApps(ctx: Context): MutableMap<String, BlockConfig> {
        val json = prefs(ctx).getString(K_APPS, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, BlockConfig>>() {}.type
        return gson.fromJson(json, type) ?: mutableMapOf()
    }

    fun blockApp(ctx: Context, pkg: String, cfg: BlockConfig) {
        val map = getBlockedApps(ctx)
        map[pkg] = cfg
        prefs(ctx).edit().putString(K_APPS, gson.toJson(map)).apply()
    }

    fun unblockApp(ctx: Context, pkg: String) {
        val map = getBlockedApps(ctx)
        map.remove(pkg)
        prefs(ctx).edit().putString(K_APPS, gson.toJson(map)).apply()
    }

    fun isAppBlocked(ctx: Context, pkg: String): Boolean {
        val cfg = getBlockedApps(ctx)[pkg] ?: return false
        return isTimeActive(cfg)
    }

    fun getAppBlockConfig(ctx: Context, pkg: String): BlockConfig? =
        getBlockedApps(ctx)[pkg]

    // ── Sites ─────────────────────────────────────────────────────────────────
    fun getBlockedSites(ctx: Context): MutableMap<String, BlockConfig> {
        val json = prefs(ctx).getString(K_SITES, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, BlockConfig>>() {}.type
        return gson.fromJson(json, type) ?: mutableMapOf()
    }

    fun blockSite(ctx: Context, domain: String, cfg: BlockConfig) {
        val map = getBlockedSites(ctx)
        map[domain.lowercase().trim()] = cfg
        prefs(ctx).edit().putString(K_SITES, gson.toJson(map)).apply()
    }

    fun unblockSite(ctx: Context, domain: String) {
        val map = getBlockedSites(ctx)
        map.remove(domain.lowercase().trim())
        prefs(ctx).edit().putString(K_SITES, gson.toJson(map)).apply()
    }

    fun isSiteBlocked(ctx: Context, domain: String): Boolean {
        val d   = domain.lowercase().removePrefix("www.")
        val map = getBlockedSites(ctx)
        val cfg = map[d]
            ?: map.keys.firstOrNull { d.endsWith(".$it") || d == it }?.let { map[it] }
            ?: return false
        return isTimeActive(cfg)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  2. ACCESSIBILITY SERVICE — App Blocker
// ════════════════════════════════════════════════════════════════════════════

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  MASTER ACCESSIBILITY SERVICE — একটিই, সব features এখানে handle হয়
 *
 *  এই single service নিচের সব কাজ করে (আলাদা service দরকার নেই):
 *  1. App Blocker (single_apps / blocker_prefs)
 *  2. Self Focus — keyword blocking, strict mode (SelfFocusAccessibilityService এর কাজ)
 *  3. Reels/Shorts Blocker (RasFocusAccessibilityService / ReelsBlockerService এর কাজ)
 *  4. Extreme Block (RasFocusBlockingService এর কাজ)
 *  5. Adult Content Blocker (BlockerAccessibilityService এর কাজ)
 *
 *  Accessibility settings এ শুধু একটি "RasFocus+" entry দেখাবে।
 * ════════════════════════════════════════════════════════════════════════════
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AppBlockerAccessibilityService? = null
    }

    private var lastBlockedPkg = ""

    // Reels/Shorts packages
    private val reelsPackages = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",  // TikTok
        "com.ss.android.ugc.trill"   // TikTok (Global)
    )

    // Adult content keywords (BlockerAccessibilityService থেকে)
    private val adultKeywords = listOf(
        "porn", "xxx", "sex", "nude", "nsfw", "sexy", "hentai", "rule34",
        "xvideos", "pornhub", "xnxx", "xhamster", "onlyfans",
        "চটি", "পর্ণ", "সেক্স", "নগ্ন", "উলঙ্গ", "যৌন", "পর্ণগ্রাফি"
    )

    // Self-focus blocked keywords (SelfFocusAccessibilityService থেকে)
    private val focusKeywords = listOf("reels", "shorts", "tiktok", "trending")

    override fun onServiceConnected() {
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        android.util.Log.i("RasFocus", "Master AccessibilityService connected ✅")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return

        // System apps skip
        if (pkg == packageName ||
            pkg == "com.android.launcher3" ||
            pkg == "com.android.systemui" ||
            pkg == "com.google.android.apps.nexuslauncher") return

        val blockerPrefs = getSharedPreferences("blocker_prefs", 0)
        val rasPrefs     = getSharedPreferences("rasfocus_prefs", 0)

        // ── 1. App Blocker ─────────────────────────────────────────────────
        val isBlockingActive = blockerPrefs.getBoolean("is_blocking_active", false)
        if (isBlockingActive && BlockedData.isAppBlocked(this, pkg)) {
            if (lastBlockedPkg != pkg) {
                lastBlockedPkg = pkg
                val cfg = BlockedData.getAppBlockConfig(this, pkg)
                if (cfg != null) {
                    val intent = Intent(this, BlockingActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("pkg",      pkg)
                        putExtra("appName",  getAppLabel(pkg))
                        putExtra("method",   cfg.method.name)
                        putExtra("password", cfg.password)
                        putExtra("longText", cfg.longText)
                        putExtra("timeFrom", cfg.timeFrom)
                        putExtra("timeTo",   cfg.timeTo)
                    }
                    startActivity(intent)
                }
            }
            return
        } else {
            if (lastBlockedPkg == pkg) lastBlockedPkg = ""
        }

        // ── 2. Extreme Block (RasFocusBlockingService delegate) ────────────
        com.rasel.RasFocus.selfcontrol.RasFocusBlockingService.instance
            ?.delegateEvent(event)

        // ── 3. Reels/Shorts Blocker ─────────────────────────────────────────
        if (pkg in reelsPackages) {
            val reelsActive = blockerPrefs.getBoolean("reels_blocking_active", false)
            if (reelsActive) {
                com.rasel.RasFocus.selfcontrol.RasFocusAccessibilityService.instance
                    ?.delegateEvent(event)
                com.rasel.RasFocus.selfcontrol.ReelsBlockerService.instance
                    ?.delegateEvent(event)
            }
        }

        // ── 4. Self Focus — keyword & strict mode ───────────────────────────
        if (isBlockingActive) {
            val isStrictMode = rasPrefs.getBoolean("strict_mode", false)
            if (isStrictMode && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (pkg.contains("packageinstaller")) {
                    val nodeText = collectNodeText(rootInActiveWindow).lowercase()
                    if (nodeText.contains("rasfocus") && nodeText.contains("uninstall")) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        android.widget.Toast.makeText(this, "Strict Mode is ON!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val isKeywordsEnabled = rasPrefs.getBoolean("keywords_enabled", false)
            if (isKeywordsEnabled && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                val nodeText = collectNodeText(rootInActiveWindow).lowercase()
                if (focusKeywords.any { nodeText.contains(it) }) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    android.widget.Toast.makeText(this, "Distracting Keyword Blocked!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── 5. Adult Content Blocker (BlockerAccessibilityService delegate) ─
        val isAdultBlockOn = rasPrefs.getBoolean("adult_block_enabled", false)
        if (isAdultBlockOn && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            com.rasel.RasFocus.features.BlockerAccessibilityService.instance
                ?.delegateEvent(event)
        }
    }

    override fun onInterrupt() {}

    private fun getAppLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    private fun collectNodeText(node: android.view.accessibility.AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        node.text?.let { sb.append(it) }
        for (i in 0 until node.childCount) sb.append(collectNodeText(node.getChild(i)))
        return sb.toString()
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  3. VPN SERVICE — Website Blocker (DNS Filtering)
// ════════════════════════════════════════════════════════════════════════════

class BlockerVpnService : VpnService() {

    private var vpnThread: Thread?                = null
    private var vpnIface:  ParcelFileDescriptor?  = null
    private val executor  = Executors.newCachedThreadPool()

    companion object {
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val DNS_SERVER  = "8.8.8.8"
        private const val DNS_PORT    = 53

        fun start(ctx: Context) {
            ctx.startService(Intent(ctx, BlockerVpnService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, BlockerVpnService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        try {
            vpnIface = Builder()
                .addAddress(VPN_ADDRESS, 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(DNS_SERVER)
                .setSession("BlockerVPN")
                .setBlocking(true)
                .establish() ?: return

            vpnThread = Thread({ runLoop() }, "VpnLoop").apply { isDaemon = true; start() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runLoop() {
        val iface     = vpnIface ?: return
        val inStream  = FileInputStream(iface.fileDescriptor)
        val outStream = FileOutputStream(iface.fileDescriptor)
        val buf       = ByteArray(32767)

        while (!Thread.interrupted()) {
            try {
                val len = inStream.read(buf)
                if (len <= 0) continue

                val pkt = ByteBuffer.wrap(buf, 0, len)

                // IPv4 only
                val ipVer = (buf[0].toInt() shr 4) and 0xF
                if (ipVer != 4) { outStream.write(buf, 0, len); continue }

                val proto = buf[9].toInt() and 0xFF
                if (proto != 17) { outStream.write(buf, 0, len); continue }   // UDP only

                val ihl     = (buf[0].toInt() and 0xF) * 4
                val dstPort = ((buf[ihl + 2].toInt() and 0xFF) shl 8) or
                               (buf[ihl + 3].toInt() and 0xFF)

                if (dstPort != DNS_PORT) { outStream.write(buf, 0, len); continue }

                // Parse DNS query payload
                val dnsData = buf.copyOfRange(ihl + 8, len)
                val domain  = parseDnsQuery(dnsData)

                if (domain != null && BlockedData.isSiteBlocked(this, domain)) {
                    // NXDOMAIN → browser gets connection error
                    val resp   = nxDomainResponse(dnsData)
                    val packet = wrapUdpIp(buf, ihl, resp)
                    outStream.write(packet)
                } else {
                    // Forward to real DNS
                    val rawPkt  = buf.copyOf(len)
                    val rawIhl  = ihl
                    executor.submit {
                        try {
                            val resp   = forwardDns(dnsData) ?: return@submit
                            val packet = wrapUdpIp(rawPkt, rawIhl, resp)
                            synchronized(outStream) { outStream.write(packet) }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                if (Thread.interrupted()) break
            }
        }
    }

    private fun parseDnsQuery(dns: ByteArray): String? = try {
        var i = 12
        buildString {
            while (i < dns.size) {
                val len = dns[i].toInt() and 0xFF
                if (len == 0) break
                if (isNotEmpty()) append('.')
                append(String(dns, i + 1, len))
                i += len + 1
            }
        }.lowercase().ifEmpty { null }
    } catch (_: Exception) { null }

    private fun nxDomainResponse(query: ByteArray): ByteArray =
        query.copyOf().also { r ->
            r[2] = (r[2].toInt() or 0x80).toByte()   // QR = response
            r[3] = (r[3].toInt() or 0x83).toByte()   // NXDOMAIN, RA
        }

    private fun forwardDns(query: ByteArray): ByteArray? {
        val sock = DatagramSocket().apply { soTimeout = 3000 }
        sock.send(DatagramPacket(query, query.size, InetAddress.getByName(DNS_SERVER), DNS_PORT))
        val resp = ByteArray(512)
        val rPkt = DatagramPacket(resp, resp.size)
        sock.receive(rPkt)
        sock.close()
        return resp.copyOf(rPkt.length)
    }

    private fun wrapUdpIp(orig: ByteArray, ihl: Int, dns: ByteArray): ByteArray {
        val udpLen   = 8 + dns.size
        val totalLen = ihl + udpLen
        val out      = ByteArray(totalLen)

        // IP header: copy then swap src/dst
        System.arraycopy(orig, 0, out, 0, ihl)
        for (i in 0..3) { out[12 + i] = orig[16 + i]; out[16 + i] = orig[12 + i] }
        out[2] = (totalLen shr 8).toByte(); out[3] = (totalLen and 0xFF).toByte()
        out[10] = 0; out[11] = 0   // clear checksum (kernel recalculates)

        // UDP header: swap ports
        out[ihl]     = orig[ihl + 2]; out[ihl + 1] = orig[ihl + 3]
        out[ihl + 2] = orig[ihl];     out[ihl + 3] = orig[ihl + 1]
        out[ihl + 4] = (udpLen shr 8).toByte(); out[ihl + 5] = (udpLen and 0xFF).toByte()
        out[ihl + 6] = 0; out[ihl + 7] = 0

        // DNS payload
        System.arraycopy(dns, 0, out, ihl + 8, dns.size)
        return out
    }

    override fun onDestroy() {
        vpnThread?.interrupt()
        vpnIface?.close()
        executor.shutdown()
        super.onDestroy()
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  4. FOREGROUND SERVICE — সবসময় চালু রাখে
// ════════════════════════════════════════════════════════════════════════════

class BlockerForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "blocker_ch"
        private const val NOTIF_ID   = 1001

        fun start(ctx: Context) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, BlockerForegroundService::class.java))
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Blocker", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps blocker running" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Blocker চালু আছে")
            .setContentText("Apps ও websites ব্লক করা হচ্ছে")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, SingleAppsActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE)
            ).build()

        startForeground(NOTIF_ID, notif)
        BlockerVpnService.start(this)   // VPN চালু করো
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null
}

// ════════════════════════════════════════════════════════════════════════════
//  5. BOOT RECEIVER — reboot এর পর auto-start
// ════════════════════════════════════════════════════════════════════════════

class SingleAppsBootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BlockerForegroundService.start(ctx)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  6. BLOCKING ACTIVITY — blocked app এর উপরে full-screen block UI
// ════════════════════════════════════════════════════════════════════════════

class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg      = intent.getStringExtra("pkg")      ?: ""
        val appName  = intent.getStringExtra("appName")  ?: pkg
        val method   = BlockMethod.valueOf(intent.getStringExtra("method") ?: "TIME_RANGE")
        val password = intent.getStringExtra("password") ?: ""
        val longText = intent.getStringExtra("longText") ?: ""
        val timeFrom = intent.getStringExtra("timeFrom") ?: ""
        val timeTo   = intent.getStringExtra("timeTo")   ?: ""

        setContent {
            MaterialTheme {
                BlockingScreen(
                    appName  = appName,
                    method   = method,
                    password = password,
                    longText = longText,
                    timeFrom = timeFrom,
                    timeTo   = timeTo,
                    onGoHome = { goHome() },
                    onUnlocked = { finish() }   // password/text সঠিক → app খুলতে দাও
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}

@Composable
fun BlockingScreen(
    appName:   String,
    method:    BlockMethod,
    password:  String,
    longText:  String,
    timeFrom:  String,
    timeTo:    String,
    onGoHome:  () -> Unit,
    onUnlocked:() -> Unit
) {
    var input    by remember { mutableStateOf("") }
    var error    by remember { mutableStateOf(false) }
    var success  by remember { mutableStateOf(false) }

    fun verify() = when (method) {
        BlockMethod.PASSWORD  -> input == password
        BlockMethod.LONG_TEXT -> input.trim().equals(longText.trim(), ignoreCase = true)
        BlockMethod.TIME_RANGE -> false
    }

    if (success) {
        LaunchedEffect(Unit) { delay(600); onUnlocked() }
        Box(Modifier.fillMaxSize().background(Color(0xFF0D9488)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(72.dp))
                Text("আনলক হয়েছে!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        return
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF0D9488)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(Modifier.height(24.dp))

            // Lock icon circle
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Text("অ্যাপ ব্লক করা আছে", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "\"$appName\" এখন ব্লক।",
                fontSize = 15.sp, color = Color.White.copy(.85f), textAlign = TextAlign.Center
            )

            // Method-specific UI
            when (method) {

                BlockMethod.TIME_RANGE -> {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(.15f)).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Text("ব্লক সময়: $timeFrom – $timeTo", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("এই সময়ের মধ্যে এই অ্যাপ ব্যবহার করা যাবে না।",
                                fontSize = 13.sp, color = Color.White.copy(.7f), textAlign = TextAlign.Center)
                        }
                    }
                }

                BlockMethod.PASSWORD -> {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it; error = false },
                        placeholder = { Text("পাসওয়ার্ড দিন", color = Color.White.copy(.55f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = error,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color.White,
                            unfocusedBorderColor = Color.White.copy(.5f),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            errorBorderColor     = Color(0xFFFF6B6B)
                        ),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) Text("পাসওয়ার্ড ভুল!", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                    Button(
                        onClick  = { if (verify()) success = true else error = true },
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("আনলক করুন", color = Color(0xFF0D9488), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }

                BlockMethod.LONG_TEXT -> {
                    Text("আনলক করতে নিচের টেক্সটটি হুবহু লিখুন:",
                        fontSize = 13.sp, color = Color.White.copy(.75f), textAlign = TextAlign.Center)
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(.15f)).padding(14.dp)
                    ) {
                        Text("\"$longText\"", fontSize = 13.sp, color = Color.White,
                            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                    OutlinedTextField(
                        value = input, onValueChange = { input = it; error = false },
                        placeholder = { Text("এখানে টাইপ করুন...", color = Color.White.copy(.5f)) },
                        isError  = error,
                        minLines = 3,
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color.White,
                            unfocusedBorderColor = Color.White.copy(.5f),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White
                        ),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) Text("টেক্সট মিলছে না!", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                    Button(
                        onClick  = { if (verify()) success = true else error = true },
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("আনলক করুন", color = Color(0xFF0D9488), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            // Home button
            OutlinedButton(
                onClick  = onGoHome,
                border   = BorderStroke(1.dp, Color.White.copy(.5f)),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("হোমে ফিরুন", color = Color.White, fontSize = 15.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  7. MAIN ACTIVITY + FULL COMPOSE UI
// ════════════════════════════════════════════════════════════════════════════

// Theme
private val Teal        = Color(0xFF0D9488)
private val TealDk      = Color(0xFF0F766E)
private val TealLt      = Color(0xFFCCFBF1)
private val TealSurf    = Color(0xFFF0FDFA)
private val TealAcc     = Color(0xFF5EEAD4)
private val RedBl       = Color(0xFFEF4444)
private val RedLt       = Color(0xFFFEE2E2)
private val TxtPri      = Color(0xFF0F172A)
private val TxtSec      = Color(0xFF64748B)
private val BdrCol      = Color(0xFFE2E8F0)
private val Wht         = Color(0xFFFFFFFF)

data class InstalledApp(val name: String, val packageName: String)

fun loadInstalledApps(ctx: Context): List<InstalledApp> {
    val pm = ctx.packageManager
    return pm.queryIntentActivities(
        android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }, PackageManager.GET_META_DATA
    )
        .map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.packageName != ctx.packageName }
        .sortedBy { it.name.lowercase() }
}

data class UiSite(val id: String, val domain: String)

class SingleAppsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePermissions()
        BlockerForegroundService.start(this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary          = Teal,
                    onPrimary        = Wht,
                    primaryContainer = TealLt,
                    surface          = Wht,
                    background       = Wht
                )
            ) { BlockerRoot() }
        }
    }

    private fun ensurePermissions() {
        if (!hasUsagePerm()) startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        if (!Settings.canDrawOverlays(this))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")))
        if (!isAccessibilityOn())
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        VpnService.prepare(this)?.let { startActivityForResult(it, 100) }
    }

    private fun hasUsagePerm(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun isAccessibilityOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains("${packageName}/${AppBlockerAccessibilityService::class.java.name}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerRoot() {
    val ctx          = LocalContext.current
    var tab          by remember { mutableStateOf(0) }
    val installed    = remember { loadInstalledApps(ctx) }
    var blockedApps  by remember { mutableStateOf(BlockedData.getBlockedApps(ctx)) }
    var blockedSites by remember { mutableStateOf(BlockedData.getBlockedSites(ctx)) }

    fun refreshApps()  { blockedApps  = BlockedData.getBlockedApps(ctx) }
    fun refreshSites() { blockedSites = BlockedData.getBlockedSites(ctx) }

    val sites = remember {
        mutableStateListOf(
            UiSite("fb_s",  "facebook.com"),
            UiSite("ig_s",  "instagram.com"),
            UiSite("tw_s",  "twitter.com"),
            UiSite("yt_s",  "youtube.com"),
            UiSite("rd_s",  "reddit.com"),
            UiSite("pin_s", "pinterest.com"),
            UiSite("eb_s",  "ebay.com")
        )
    }

    var showModal     by remember { mutableStateOf(false) }
    var pendingPkg    by remember { mutableStateOf<String?>(null) }
    var pendingDomain by remember { mutableStateOf<String?>(null) }
    var pendingName   by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == 0) "All Apps" else "All Sites", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TxtPri) },
                navigationIcon = { 
                    IconButton(onClick = { (ctx as? ComponentActivity)?.finish() }) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Dashboard", tint = TxtPri) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Wht)
            )
        },
        containerColor = Wht
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {

            TabRow(selectedTabIndex = tab, containerColor = Wht, contentColor = Teal) {
                listOf("Apps", "Sites").forEachIndexed { i, lbl ->
                    Tab(
                        selected = tab == i, onClick = { tab = i },
                        text = { Text(lbl, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal) },
                        selectedContentColor = Teal, unselectedContentColor = TxtSec
                    )
                }
            }

            when (tab) {
                0 -> AppsTab(
                    apps         = installed,
                    blockedApps  = blockedApps,
                    onBlock      = { app -> pendingPkg = app.packageName; pendingDomain = null; pendingName = app.name; showModal = true },
                    onUnblock    = { app -> BlockedData.unblockApp(ctx, app.packageName); refreshApps() }
                )
                1 -> SitesTab(
                    sites        = sites,
                    blockedSites = blockedSites,
                    onBlock      = { s -> pendingPkg = null; pendingDomain = s.domain; pendingName = s.domain; showModal = true },
                    onUnblock    = { s -> BlockedData.unblockSite(ctx, s.domain); refreshSites() },
                    onAddSite    = { domain ->
                        sites.add(0, UiSite("c_${System.currentTimeMillis()}", domain))
                        pendingPkg = null; pendingDomain = domain; pendingName = domain; showModal = true
                    }
                )
            }
        }
    }

    if (showModal) {
        BlockDialog(
            name      = pendingName,
            onDismiss = { showModal = false },
            onConfirm = { cfg ->
                pendingPkg?.let    { BlockedData.blockApp(ctx, it, cfg);  refreshApps() }
                pendingDomain?.let { BlockedData.blockSite(ctx, it, cfg); refreshSites() }
                showModal = false
            }
        )
    }
}

// ── Apps Tab ──────────────────────────────────────────────────────────────────
@Composable
fun AppsTab(
    apps:        List<InstalledApp>,
    blockedApps: Map<String, BlockConfig>,
    onBlock:     (InstalledApp) -> Unit,
    onUnblock:   (InstalledApp) -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered  = apps.filter { it.name.contains(q, ignoreCase = true) }
    val blocked   = filtered.filter {  blockedApps.containsKey(it.packageName) }
    val unblocked = filtered.filter { !blockedApps.containsKey(it.packageName) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { SearchBox(q, "Search apps...") { q = it } }
        if (blocked.isNotEmpty()) {
            item { SecLabel("Blocked (${blocked.size})") }
            items(blocked, key = { it.packageName }) { app ->
                ItemCard(app.name, blockedApps[app.packageName], true) { onUnblock(app) }
            }
        }
        items(unblocked, key = { it.packageName }) { app ->
            ItemCard(app.name, null, false) { onBlock(app) }
        }
    }
}

// ── Sites Tab ─────────────────────────────────────────────────────────────────
@Composable
fun SitesTab(
    sites:       List<UiSite>,
    blockedSites:Map<String, BlockConfig>,
    onBlock:     (UiSite) -> Unit,
    onUnblock:   (UiSite) -> Unit,
    onAddSite:   (String) -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered  = sites.filter { it.domain.contains(q, ignoreCase = true) }
    val blocked   = filtered.filter {  blockedSites.containsKey(it.domain) }
    val unblocked = filtered.filter { !blockedSites.containsKey(it.domain) }
    val showAdd   = q.isNotBlank() && !sites.any { it.domain.contains(q.lowercase()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { SearchBox(q, "Search / Add website...") { q = it } }
        if (showAdd) item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(TealSurf)
                    .border(1.dp, Teal.copy(.3f), RoundedCornerShape(14.dp))
                    .clickable { onAddSite(q.trim()); q = "" }
                    .padding(14.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(TealLt), contentAlignment = Alignment.Center) {
                    Text("🌐", fontSize = 20.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Add \"$q\"", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Teal)
                    Text("Tap to add and block", fontSize = 12.sp, color = TealDk.copy(.7f))
                }
                Icon(Icons.Default.Add, null, tint = Teal, modifier = Modifier.size(20.dp))
            }
        }
        if (blocked.isNotEmpty()) {
            item { SecLabel("Blocked (${blocked.size})") }
            items(blocked, key = { it.id }) { s ->
                ItemCard(s.domain, blockedSites[s.domain], true) { onUnblock(s) }
            }
        }
        items(unblocked, key = { it.id }) { s ->
            ItemCard(s.domain, null, false) { onBlock(s) }
        }
    }
}

// ── Shared Item Card ──────────────────────────────────────────────────────────
@Composable
fun ItemCard(name: String, cfg: BlockConfig?, isBlocked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isBlocked) RedLt else Wht)
            .border(if (isBlocked) 1.dp else 0.5.dp,
                if (isBlocked) RedBl.copy(.35f) else BdrCol, RoundedCornerShape(14.dp))
            .padding(14.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (isBlocked) RedBl.copy(.1f) else TealSurf),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isBlocked) Icons.Default.Lock else Icons.Default.Apps,
                null, tint = if (isBlocked) RedBl else Teal, modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TxtPri,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (cfg != null) Badge(cfg)
            }
            Text(
                if (isBlocked) "🔴 Blocked" else "Spent: 00 sec",
                fontSize = 12.sp, color = if (isBlocked) RedBl else TxtSec
            )
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(if (isBlocked) RedLt else TealSurf)
                .border(0.5.dp, if (isBlocked) RedBl.copy(.3f) else TealAcc.copy(.4f), CircleShape)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
                if (isBlocked) "Unblock" else "Block",
                tint = if (isBlocked) RedBl else Teal, modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Block Modal ───────────────────────────────────────────────────────────────
@Composable
fun BlockDialog(name: String, onDismiss: () -> Unit, onConfirm: (BlockConfig) -> Unit) {
    var method   by remember { mutableStateOf(BlockMethod.TIME_RANGE) }
    var timeFrom by remember { mutableStateOf("09:00") }
    var timeTo   by remember { mutableStateOf("17:00") }
    var password by remember { mutableStateOf("") }
    var longText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Wht, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(TealLt), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, null, tint = Teal, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Block", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TxtPri)
                        Text(name, fontSize = 13.sp, color = TxtSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider(color = BdrCol)
                Text("Block method", fontSize = 13.sp, color = TxtSec, fontWeight = FontWeight.Medium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("⏰ Time" to BlockMethod.TIME_RANGE, "🔑 Pass" to BlockMethod.PASSWORD, "📝 Text" to BlockMethod.LONG_TEXT)
                        .forEach { (lbl, m) ->
                            Box(
                                Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (method == m) Teal else TealSurf)
                                    .border(0.5.dp, if (method == m) Teal else BdrCol, RoundedCornerShape(20.dp))
                                    .clickable { method = m }
                                    .padding(14.dp, 8.dp)
                            ) {
                                Text(lbl, fontSize = 13.sp, color = if (method == m) Wht else TxtSec,
                                    fontWeight = if (method == m) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                }

                AnimatedContent(method, label = "mi") { m ->
                    when (m) {
                        BlockMethod.TIME_RANGE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Time range", fontSize = 13.sp, color = TxtSec)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("From" to timeFrom, "To" to timeTo).forEachIndexed { i, (lbl, v) ->
                                    OutlinedTextField(
                                        value = v, onValueChange = { if (i == 0) timeFrom = it else timeTo = it },
                                        label = { Text(lbl) }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = BdrCol),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        BlockMethod.PASSWORD -> OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Password") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = BdrCol),
                            modifier = Modifier.fillMaxWidth()
                        )
                        BlockMethod.LONG_TEXT -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Type this to unblock", fontSize = 13.sp, color = TxtSec)
                            OutlinedTextField(
                                value = longText, onValueChange = { longText = it },
                                placeholder = { Text("e.g. I will stay focused...", fontSize = 13.sp, color = TxtSec) },
                                minLines = 3, maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = BdrCol),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(color = BdrCol)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, BdrCol),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TxtSec),
                        modifier = Modifier.weight(1f)) { Text("Cancel") }

                    Button(
                        onClick = {
                            onConfirm(when (method) {
                                BlockMethod.TIME_RANGE -> BlockConfig(method, timeFrom, timeTo)
                                BlockMethod.PASSWORD   -> BlockConfig(method, password = password)
                                BlockMethod.LONG_TEXT  -> BlockConfig(method, longText = longText)
                            })
                        },
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedBl),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Block")
                    }
                }
            }
        }
    }
}

// ── Micro composables ─────────────────────────────────────────────────────────
@Composable
fun SearchBox(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = TxtSec) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = TxtSec) },
        trailingIcon = if (value.isNotEmpty()) { { IconButton({ onValueChange("") }) {
            Icon(Icons.Default.Close, null, tint = TxtSec, modifier = Modifier.size(18.dp))
        } } } else null,
        shape = RoundedCornerShape(12.dp), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Teal, unfocusedBorderColor = BdrCol,
            focusedContainerColor = Wht, unfocusedContainerColor = Wht
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SecLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TxtSec,
        letterSpacing = 0.8.sp, modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp))
}

@Composable
fun Badge(cfg: BlockConfig) {
    val (lbl, bg, tc) = when (cfg.method) {
        BlockMethod.TIME_RANGE -> Triple("${cfg.timeFrom}–${cfg.timeTo}", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        BlockMethod.PASSWORD   -> Triple("🔑 Pass", Color(0xFFF0FDF4), Color(0xFF15803D))
        BlockMethod.LONG_TEXT  -> Triple("📝 Text", RedLt, RedBl)
    }
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(7.dp, 2.dp)) {
        Text(lbl, fontSize = 10.sp, color = tc, fontWeight = FontWeight.SemiBold)
    }
}