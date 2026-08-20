package app.foldzoom.diagnostic

import android.os.IBinder
import android.os.Parcel

/** Calls the same hidden WindowManager operation used by `wm density` / Developer Options. */
object WindowManagerDensity {
    fun apply(displayId: Int, density: Int): String {
        return runCatching {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getMethod("getService", String::class.java)
            val windowBinder = getService.invoke(null, "window") as? IBinder
                ?: error("WindowManager Binder is unavailable")

            val request = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                request.writeInterfaceToken(WINDOW_MANAGER_DESCRIPTOR)
                request.writeInt(displayId)
                request.writeInt(density)
                request.writeInt(PRIMARY_USER_ID)
                check(windowBinder.transact(TRANSACTION_SET_FORCED_DISPLAY_DENSITY_FOR_USER, request, reply, 0)) {
                    "WindowManager rejected the density transaction"
                }
                reply.readException()
            } finally {
                request.recycle()
                reply.recycle()
            }
            "WindowManager applied $density dpi to display $displayId. The screen should reconfigure now."
        }.getOrElse {
            "Live WindowManager call failed: ${it.cause?.javaClass?.simpleName ?: it.javaClass.simpleName}: ${it.cause?.message ?: it.message}"
        }
    }

    private const val WINDOW_MANAGER_DESCRIPTOR = "android.view.IWindowManager"
    private const val PRIMARY_USER_ID = 0

    // Verified from this Fold 7's One UI 8.5 framework: IWindowManager#setForcedDisplayDensityForUser.
    private const val TRANSACTION_SET_FORCED_DISPLAY_DENSITY_FOR_USER = 75
}
