package app.foldzoom.diagnostic

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var angle: TextView
    private lateinit var coverDensity: EditText
    private lateinit var innerDensity: EditText
    private lateinit var grantButton: Button
    private lateinit var grantStatus: TextView
    private lateinit var testButton: Button
    private lateinit var batteryButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var monitor: HingeMonitor
    private lateinit var shizukuSetup: ShizukuSetup
    private var receiverRegistered = false

    private val prefs by lazy { getSharedPreferences("presets", MODE_PRIVATE) }

    private val postureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra(HingeMonitorService.EXTRA_ANGLE)) {
                showPosture(intent.getFloatExtra(HingeMonitorService.EXTRA_ANGLE, -1f), intent.getStringExtra(HingeMonitorService.EXTRA_POSTURE) ?: "UNKNOWN")
            }
            intent.getStringExtra(HingeMonitorService.EXTRA_RESULT)?.let(::setStatus)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        monitor = HingeMonitor(this) { degrees, posture -> runOnUiThread { showPosture(degrees, posture.name) } }
        shizukuSetup = ShizukuSetup(this) { message ->
            runOnUiThread {
                grantStatus.text = message
                refreshUiState()
            }
        }
        refreshStatus()
    }

    override fun onStart() {
        super.onStart()
        monitor.start()
        registerReceiver(postureReceiver, IntentFilter(HingeMonitorService.ACTION_POSTURE), RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    override fun onResume() {
        super.onResume()
        if (::grantButton.isInitialized) refreshUiState()
    }

    override fun onStop() {
        monitor.stop()
        if (receiverRegistered) unregisterReceiver(postureReceiver)
        receiverRegistered = false
        super.onStop()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(36), dp(16), dp(32))
        }
        scroll.addView(root)

        root.addView(text("ZFold Multi DPI", 26f, bold = true))
        root.addView(text("Separate Screen Zoom for cover and inner displays", 15f, color = Color.DKGRAY))

        val statusCard = card("Status")
        status = text("")
        angle = text("Hinge: waiting for sensor…")
        statusCard.addView(status)
        statusCard.addView(angle)
        statusCard.addView(button("Refresh status") {
            refreshStatus()
            toast("Status refreshed")
        })
        root.addView(statusCard)

        val presetsCard = card("Presets")
        presetsCard.addView(text("Save the target DPI for each stable posture."))
        coverDensity = input(presetsCard, "Cover-screen DPI", prefs.getInt("cover", 410))
        innerDensity = input(presetsCard, "Inner-screen DPI", prefs.getInt("inner", 380))
        presetsCard.addView(button("Save presets") {
            prefs.edit()
                .putInt("cover", coverDensity.value())
                .putInt("inner", innerDensity.value())
                .putBoolean("presets_saved", true)
                .putBoolean("live_verified", false)
                .remove("last_applied")
                .apply()
            setStatus("Presets saved. Continue with the setup steps below.")
            toast("Saved!")
            refreshUiState()
        })
        root.addView(presetsCard)

        val setupCard = card("Setup & Automation")
        setupCard.addView(text("Complete each step in order. Buttons unlock when their prerequisite is ready."))
        grantButton = button("1. Grant secure-settings access through Shizuku") {
            toast("Requesting Shizuku access…")
            grantStatus.text = "Checking Shizuku connection…"
            shizukuSetup.requestAccessAndGrant()
        }
        grantStatus = text("", 14f, color = Color.DKGRAY)
        testButton = button("2. Apply preset settings") { applyLiveWindowManagerDensity() }
        batteryButton = button("3. Allow unrestricted battery use") { requestBatteryExemption() }
        startButton = button("Start automatic DPI switching") { startPostureMonitor() }
        stopButton = button("Stop automatic DPI switching") { stopPostureMonitor() }
        setupCard.addView(grantButton)
        setupCard.addView(grantStatus)
        setupCard.addView(testButton)
        setupCard.addView(batteryButton)
        setupCard.addView(startButton)
        setupCard.addView(stopButton)
        setupCard.addView(text("Automation waits 700 ms at fully folded or fully unfolded before applying the saved DPI. The persistent notification is required by Android while it runs.", 14f, color = Color.DKGRAY))
        root.addView(setupCard)

        setContentView(scroll)
    }

    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(14))
        background = GradientDrawable().apply {
            setColor(Color.rgb(248, 249, 252))
            setStroke(dp(1), Color.rgb(215, 220, 230))
            cornerRadius = dp(16).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
        }
        addView(text(title, 19f, bold = true))
    }

    private fun text(value: String, size: Float = 16f, bold: Boolean = false, color: Int = Color.BLACK) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        gravity = Gravity.CENTER
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(4)
        }
    }

    private fun input(parent: LinearLayout, label: String, value: Int): EditText {
        parent.addView(text(label))
        return EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(value.toString())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            parent.addView(this)
        }
    }

    private fun applyLiveWindowManagerDensity() {
        if (!hasSecureSettings()) return setStatus("Complete step 1 first.")
        val value = targetDensityForCurrentPosture()
        val result = WindowManagerDensity.apply(display.displayId, value)
        if (result.startsWith("WindowManager applied")) {
            prefs.edit().putBoolean("live_verified", true).apply()
            toast("Applied")
        } else {
            toast("Could not apply preset")
        }
        setStatus(result)
        refreshUiState()
    }

    private fun startPostureMonitor() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 5)
        }
        startForegroundService(Intent(this, HingeMonitorService::class.java))
        prefs.edit().putBoolean("monitor_requested", true).apply()
        setStatus("Automatic switching started. Fold or unfold fully and pause briefly.")
        toast("Automatic switching started")
        refreshUiState()
    }

    private fun stopPostureMonitor() {
        stopService(Intent(this, HingeMonitorService::class.java))
        prefs.edit().putBoolean("monitor_requested", false).apply()
        setStatus("Automatic posture switching stopped.")
        toast("Automatic switching stopped")
        refreshUiState()
    }

    private fun requestBatteryExemption() {
        if (isIgnoringBatteryOptimizations()) {
            setStatus("Battery optimization is already disabled for this app.")
            return toast("Battery use is already unrestricted")
        }
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        runCatching { startActivity(request) }.onFailure {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        setStatus("Approve the system battery prompt, then return here.")
        toast("Opening battery settings")
    }

    private fun refreshStatus() {
        setStatus("Secure settings: ${if (hasSecureSettings()) "granted" else "not granted"}\nBattery: ${if (isIgnoringBatteryOptimizations()) "unrestricted" else "optimized"}\nCurrent density: ${resources.displayMetrics.densityDpi} dpi")
        refreshUiState()
    }

    private fun refreshUiState() {
        val secure = hasSecureSettings()
        val monitoring = prefs.getBoolean("monitor_requested", false)
        grantButton.isEnabled = !secure
        grantButton.text = if (secure) "1. Secure-settings access granted" else "1. Grant secure-settings access through Shizuku"
        if (grantStatus.text.isBlank()) {
            grantStatus.text = if (secure) "Secure-settings access is granted." else "Grant this once through Shizuku to unlock the remaining steps."
        }
        testButton.isEnabled = secure
        testButton.text = "2. Apply preset settings"
        batteryButton.isEnabled = secure && !isIgnoringBatteryOptimizations()
        batteryButton.text = if (isIgnoringBatteryOptimizations()) "3. Battery use is unrestricted" else "3. Allow unrestricted battery use"
        startButton.isEnabled = secure && !monitoring
        stopButton.isEnabled = monitoring
    }

    private fun targetDensityForCurrentPosture() = if (angle.text.contains("UNFOLDED")) innerDensity.value() else coverDensity.value()
    private fun hasSecureSettings() = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    private fun isIgnoringBatteryOptimizations() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    private fun setStatus(message: String) { status.text = message }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun showPosture(degrees: Float, posture: String) { angle.text = "Hinge: ${degrees.roundToInt()}° — $posture" }
    private fun EditText.value(): Int = text.toString().toIntOrNull()?.coerceIn(160, 1000) ?: 0
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
