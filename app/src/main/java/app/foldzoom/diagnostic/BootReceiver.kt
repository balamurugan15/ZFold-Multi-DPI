package app.foldzoom.diagnostic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Restores user-enabled posture switching after a reboot or an in-place app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val enabled = context.getSharedPreferences("presets", Context.MODE_PRIVATE)
            .getBoolean("monitor_requested", false)
        if (!enabled) return

        val serviceIntent = Intent(context, HingeMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
