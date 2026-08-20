package app.foldzoom.diagnostic

import android.util.Log

/**
 * Launched by Shizuku in its shell-identity process. This is deliberately an AIDL Binder,
 * not an Android Service; UserService classes are created directly by Shizuku.
 */
class GrantUserService : IGrantUserService.Stub() {
    override fun grant(packageName: String): String {
        require(packageName == PACKAGE_NAME) { "Refusing a grant for another package." }
        return runCatching {
            Log.d(TAG, "Executing secure-settings grant as uid=${android.os.Process.myUid()}")
            val process = ProcessBuilder("/system/bin/pm", "grant", packageName, "android.permission.WRITE_SECURE_SETTINGS")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            if (exitCode == 0) "Secure-settings grant completed. Stop Shizuku and run the direct-write test." else "Grant failed ($exitCode): $output"
        }.getOrElse {
            Log.e(TAG, "Unable to execute secure-settings grant", it)
            "Grant command could not run: ${it.javaClass.simpleName}: ${it.message}"
        }
    }

    companion object {
        private const val TAG = "FoldZoom"
        private const val PACKAGE_NAME = "app.foldzoom.diagnostic"
    }
}
