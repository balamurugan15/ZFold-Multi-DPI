package app.foldzoom.diagnostic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Display

class HingeMonitorService : Service() {
    private lateinit var monitor: HingeMonitor
    private val handler = Handler(Looper.getMainLooper())
    private var pendingApply: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "ZFold Multi DPI monitoring", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("ZFold Multi DPI is monitoring posture")
            .setContentText("Automatic cover/inner Screen Zoom switching is active.")
            .build())
        monitor = HingeMonitor(this) { angle, posture ->
            sendBroadcast(Intent(ACTION_POSTURE).setPackage(packageName)
                .putExtra(EXTRA_ANGLE, angle)
                .putExtra(EXTRA_POSTURE, posture.name))
            scheduleDensityApply(posture)
        }
        monitor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        pendingApply?.let(handler::removeCallbacks)
        monitor.stop()
        super.onDestroy()
    }

    private fun scheduleDensityApply(posture: FoldPosture) {
        pendingApply?.let(handler::removeCallbacks)
        if (posture != FoldPosture.FOLDED && posture != FoldPosture.UNFOLDED) return
        pendingApply = Runnable {
            if (checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                report("Automatic switch skipped: WRITE_SECURE_SETTINGS is no longer granted.")
                return@Runnable
            }
            val prefs = getSharedPreferences("presets", MODE_PRIVATE)
            val density = if (posture == FoldPosture.FOLDED) prefs.getInt("cover", 410) else prefs.getInt("inner", 380)
            if (prefs.getInt("last_applied", -1) == density) {
                report("$posture already uses $density dpi.")
                return@Runnable
            }
            val result = WindowManagerDensity.apply(Display.DEFAULT_DISPLAY, density)
            if (result.startsWith("WindowManager applied")) prefs.edit().putInt("last_applied", density).apply()
            report("$posture: $result")
        }
        handler.postDelayed(pendingApply!!, APPLY_DEBOUNCE_MS)
    }

    private fun report(message: String) {
        getSystemService(NotificationManager::class.java).notify(1, android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("ZFold Multi DPI is monitoring posture")
            .setContentText(message)
            .build())
        sendBroadcast(Intent(ACTION_POSTURE).setPackage(packageName).putExtra(EXTRA_RESULT, message))
    }

    companion object {
        const val CHANNEL_ID = "hinge-monitor"
        const val ACTION_POSTURE = "app.foldzoom.diagnostic.POSTURE"
        const val EXTRA_ANGLE = "angle"
        const val EXTRA_POSTURE = "posture"
        const val EXTRA_RESULT = "result"
        private const val APPLY_DEBOUNCE_MS = 700L
    }
}
