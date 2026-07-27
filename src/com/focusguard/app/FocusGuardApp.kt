package com.focusguard.app

import android.app.*
import android.content.*
import android.content.pm.*
import android.graphics.Color
import android.net.*
import android.os.*
import android.provider.Settings
import android.text.format.DateUtils
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        hideSystemBars()

        createNotificationChannels()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = ComposeColor(0xFFBB86FC),
                    secondary = ComposeColor(0xFF03DAC5),
                    background = ComposeColor.Black,
                    surface = ComposeColor(0xFF1E1E1E),
                    onPrimary = ComposeColor.Black,
                    onSecondary = ComposeColor.Black,
                    onBackground = ComposeColor.White,
                    onSurface = ComposeColor.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = ComposeColor.Black) {
                    FocusGuardApp(this@MainActivity)
                }
            }
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                getString(R.string.notification_channel_timer),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Focus timer running" }

            val vpnChannel = NotificationChannel(
                CHANNEL_VPN,
                getString(R.string.notification_channel_vpn),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "VPN blocking service" }

            getSystemService(NotificationManager::class.java).createNotificationChannels(
                listOf(timerChannel, vpnChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_TIMER = "timer_channel"
        const val CHANNEL_VPN = "vpn_channel"
        const val ACTION_START_TIMER = "com.focusguard.START_TIMER"
        const val ACTION_STOP_TIMER = "com.focusguard.STOP_TIMER"
        const val ACTION_TOGGLE_VPN = "com.focusguard.TOGGLE_VPN"
        const val EXTRA_MINUTES = "extra_minutes"
    }
}

class FocusGuardApp(private val activity: MainActivity) : ComponentActivity() {
    @Composable
    fun AppUI() {
        var selectedTab by remember { mutableIntStateOf(0) }

        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = ComposeColor(0xFF1E1E1E), contentColor = ComposeColor.White) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Usage") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Internet") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Timer") })
            }

            when (selectedTab) {
                0 -> UsageTab(activity)
                1 -> InternetTab(activity)
                2 -> TimerTab(activity)
            }
        }
    }

    @Composable
    fun UsageTab(context: Context) {
        var apps by remember { mutableStateOf<List<UsageEntry>>(emptyList()) }
        var hasPermission by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            hasPermission = hasUsageStatsPermission(context)
            if (hasPermission) loadTopApps(context)
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (!hasPermission) {
                Text("Usage stats permission required", color = ComposeColor(0xFFD32F2F), fontSize = 18.sp, modifier = Modifier.padding(16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF6200EE))
                ) {
                    Text("Grant Permission")
                }
            } else {
                Text("Top 5 Apps Today", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp), color = ComposeColor.White)
                if (apps.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                    Text("Loading...", modifier = Modifier.align(Alignment.CenterHorizontally), color = ComposeColor.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(apps) { app ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF2D2D2D))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = app.name, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = ComposeColor.White)
                                        Text(text = formatDuration(app.totalTime), color = ComposeColor(0xFF03DAC5), fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Last used: ${DateUtils.getRelativeTimeSpanString(app.lastUsed, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)}",
                                        color = ComposeColor.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                        progress = { app.totalTime.toFloat() / apps.first().totalTime },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = ComposeColor(0xFF03DAC5),
                                        trackColor = ComposeColor(0xFF404040)
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun InternetTab(context: Context) {
        var isBlocked by remember { mutableStateOf(false) }
        var hasPermission by remember { mutableStateOf(false) }
        var vpnRunning by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            hasPermission = hasWriteSecureSettingsPermission(context)
            vpnRunning = (context.getSystemService(Context.VPN_SERVICE) as VpnManager?)?.let { false } ?: false
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier.size(200.dp).background(if (isBlocked) ComposeColor(0xFFD32F2F) else ComposeColor(0xFF2D2D2D), shape = MaterialTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBlocked) "OFFLINE" else "ONLINE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBlocked) ComposeColor.White else ComposeColor(0xFF03DAC5)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (!isBlocked) blockInternet(context) else restoreInternet(context)
                    isBlocked = !isBlocked
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlocked) ComposeColor(0xFF03DAC5) else ComposeColor(0xFFD32F2F),
                    contentColor = ComposeColor.Black
                )
            ) {
                Text(if (isBlocked) "Restore Internet" else "Block Internet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF2D2D2D))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = true, onClick = {}, colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFF03DAC5)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VPN-based blocking (no root)", color = ComposeColor.White)
                    }
                }
            }
        }
    }

    @Composable
    fun TimerTab(context: Context) {
        var minutes by remember { mutableIntStateOf(25) }
        var isRunning by remember { mutableStateOf(false) }
        var remainingSeconds by remember { mutableIntStateOf(0) }
        var notificationActive by remember { mutableStateOf(false) }

        LaunchedEffect(isRunning) {
            if (isRunning && remainingSeconds > 0) {
                while (remainingSeconds > 0) {
                    delay(1000L)
                    remainingSeconds--
                }
                if (remainingSeconds == 0) {
                    isRunning = false
                    restoreInternet(context)
                    showTimerCompleteNotification(context)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = if (isRunning) formatTime(remainingSeconds) else "${minutes} min",
                fontSize = 96.sp,
                fontWeight = FontWeight.Light,
                color = if (isRunning) ComposeColor(0xFFBB86FC) else ComposeColor.White,
                modifier = Modifier.padding(32.dp)
            )

            if (!isRunning) {
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = it.toInt() },
                    valueRange = 1f..120f,
                    steps = 119,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ComposeColor(0xFFBB86FC),
                        activeTrackColor = ComposeColor(0xFFBB86FC)
                    )
                )
                Text("${minutes} min", modifier = Modifier.padding(top = 8.dp), color = ComposeColor.Gray)

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        remainingSeconds = minutes * 60
                        isRunning = true
                        startFocusSession(context, minutes)
                        acquireWakeLock(context)
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFBB86FC), contentColor = ComposeColor.Black)
                ) {
                    Text("START FOCUS", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        isRunning = false
                        stopFocusSession(context)
                        releaseWakeLock()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFD32F2F), contentColor = ComposeColor.White)
                ) {
                    Text("STOP", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = true, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = ComposeColor(0xFF03DAC5)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keep screen on", color = ComposeColor.Gray)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }
}

data class UsageEntry(val name: String, val packageName: String, val totalTime: Long, val lastUsed: Long)

object UsageTracker {
    fun getTopApps(context: Context, limit: Int = 5): List<UsageEntry> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()) ?: return emptyList()
        return stats.filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map {
                val pm = context.packageManager
                val appInfo = runCatching { pm.getApplicationInfo(it.packageName, 0) }.getOrNull()
                val label = appInfo?.let { a -> pm.getApplicationLabel(a).toString() } ?: it.packageName
                UsageEntry(label, it.packageName, it.totalTimeInForeground, it.lastTimeUsed)
            }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 1000, now)
        return !stats.isNullOrEmpty()
    }

    fun RequestPermission(context: Context) {
        try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (e: Exception) {}
    }
}

object InternetController {
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else { @Suppress("DEPRECATION") true }
    }

    fun isAirplaneModeOn(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } else {
            @Suppress("DEPRECATION") Settings.System.getInt(context.contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) != 0
        }
    }

    fun setAirplaneMode(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (enabled) 1 else 0)
        } else {
            @Suppress("DEPRECATION") Settings.System.putInt(context.contentResolver, Settings.System.AIRPLANE_MODE_ON, if (enabled) 1 else 0)
        }
        context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", enabled))
    }

    fun blockViaVpn(context: Context): Intent {
        return Intent(context, BlockerVpnService::class.java).apply { action = BlockerVpnService.ACTION_CONNECT }
    }

    fun unblockViaVpn(context: Context): Intent {
        return Intent(context, BlockerVpnService::class.java).apply { action = BlockerVpnService.ACTION_DISCONNECT }
    }
}

class BlockerVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID_VPN,
            buildNotification("FocusGuard VPN Running", "Blocking all internet traffic")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT, ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return
        try {
            vpnInterface = Builder()
                .setSession("FocusGuard Blocker")
                .setConfigureIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .establish()
            isRunning = true
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            isRunning = false
        } catch (e: Exception) { e.printStackTrace() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, FocusGuardApp.CHANNEL_VPN)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_CONNECT = "com.focusguard.VPN_CONNECT"
        const val ACTION_DISCONNECT = "com.focusguard.VPN_DISCONNECT"
        const val ACTION_STOP = "com.focusguard.VPN_STOP"
        const val NOTIFICATION_ID_VPN = 1001
    }
}

class TimerForegroundService : Service() {
    private var countdownJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID_TIMER, buildNotification("Focus Timer Running", "Stay focused!"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val minutes = intent?.getIntExtra(FocusGuardApp.EXTRA_MINUTES, 25) ?: 25
        startTimer(minutes)
        return START_STICKY
    }

    private fun startTimer(minutes: Int) {
        countdownJob?.cancel()
        val totalSeconds = minutes * 60
        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            for (s in totalSeconds downTo 1) {
                updateNotification(formatTime(s))
                delay(1000L)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, FocusGuardApp.CHANNEL_TIMER)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(time: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_TIMER, buildNotification("Focus Timer Running", "Time left: $time"))
    }

    companion object {
        const val NOTIFICATION_ID_TIMER = 1002
    }
}

object SystemUI {
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquireWakeLock(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "FocusGuard:TimerLock")
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun releaseWakeLock() {
        try { wakeLock?.release(); wakeLock = null } catch (e: Exception) {}
    }
}

fun hasUsageStatsPermission(context: Context): Boolean = UsageTracker.hasUsageStatsPermission(context)
fun hasWriteSecureSettingsPermission(context: Context): Boolean = InternetController.hasWriteSecureSettingsPermission(context)
fun getTopApps(context: Context, limit: Int = 5): List<UsageEntry> = UsageTracker.getTopApps(context, limit)
fun RequestPermission(context: Context) = UsageTracker.RequestPermission(context)
fun blockInternet(context: Context) {
    val startIntent = InternetController.blockViaVpn(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(startIntent)
    } else {
        context.startService(startIntent)
    }
}
fun restoreInternet(context: Context) {
    val stopIntent = InternetController.unblockViaVpn(context)
    context.stopService(stopIntent)
    if (InternetController.hasWriteSecureSettingsPermission(context)) {
        InternetController.setAirplaneMode(context, false)
    }
}
fun startFocusSession(context: Context, minutes: Int) {
    val intent = Intent(context, TimerForegroundService::class.java).apply { putExtra(FocusGuardApp.EXTRA_MINUTES, minutes) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}
fun stopFocusSession(context: Context) {
    context.stopService(Intent(context, TimerForegroundService::class.java))
}
fun acquireWakeLock(context: Context) = SystemUI.acquireWakeLock(context)
fun releaseWakeLock() = SystemUI.releaseWakeLock()
fun showTimerCompleteNotification(context: Context) {
    val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("complete", "Timer Complete", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }
    val builder = NotificationCompat.Builder(context, "complete")
        .setContentTitle("Focus Session Complete!")
        .setContentText("Internet has been restored.")
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
    manager.notify(3000, builder.build())
}
fun formatDuration(ms: Long): String {
    val totalMins = ms / 60000
    val hrs = totalMins / 60
    val mins = (totalMins % 60).toInt()
    val secs = ((ms % 60000) / 1000).toInt()
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m ${secs}s"
}
fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
