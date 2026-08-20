package app.foldzoom.diagnostic

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs

/** Runs only during setup. Runtime automation must not depend on this service. */
class ShizukuSetup(private val context: Context, private val onResult: (String) -> Unit) {
    private var connection: ServiceConnection? = null
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onResult("Shizuku access approved. Applying the one-time secure-settings grant…")
            bindGrantService()
        } else {
            onResult("Shizuku access was not granted. Approve the request in Shizuku, then try again.")
        }
    }

    init {
        Shizuku.addRequestPermissionResultListener(permissionListener)
    }

    fun requestAccessAndGrant() {
        val binderReady = Shizuku.pingBinder()
        val apiPermission = if (binderReady) Shizuku.checkSelfPermission() else android.content.pm.PackageManager.PERMISSION_DENIED
        Log.d(TAG, "Grant requested: binderReady=$binderReady apiPermission=$apiPermission")
        if (binderReady && apiPermission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onResult("Shizuku connected and authorized. Starting its one-time grant service…")
            bindGrantService()
        } else if (binderReady) {
            Shizuku.requestPermission(REQUEST_CODE)
            onResult("Waiting for Shizuku approval…")
        } else {
            onResult("Start Shizuku first, then return and tap Grant again.")
        }
    }

    private fun bindGrantService() {
        val args = UserServiceArgs(ComponentName(context, GrantUserService::class.java))
            .daemon(false)
            .processNameSuffix("secure-grant")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(TAG, "Shizuku grant service connected: $name")
                onResult("Shizuku grant service connected. Executing /system/bin/pm…")
                val grant = IGrantUserService.Stub.asInterface(service)
                val result = runCatching { grant.grant(context.packageName) }
                    .getOrElse { "Grant failed: ${it.message}" }
                Log.d(TAG, "Grant result: $result")
                onResult(result)
                Shizuku.unbindUserService(args, this, true)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(TAG, "Shizuku grant service disconnected: $name")
            }
        }
        runCatching { Shizuku.bindUserService(args, connection!!) }
            .onFailure {
                Log.e(TAG, "Could not start Shizuku grant service", it)
                onResult("Could not start the Shizuku setup service: ${it.message}")
            }
    }

    companion object {
        const val REQUEST_CODE = 8421
        private const val TAG = "FoldZoom"
    }
}
