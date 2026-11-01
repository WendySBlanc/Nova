package eu.kanade.tachiyomi.extension.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class ExtensionUninstallActivity : Activity() {

    private lateinit var packageNames: ArrayList<String>
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        packageNames = savedInstanceState?.getStringArrayList(STATE_PACKAGE_NAMES)
            ?: intent.getStringArrayListExtra(EXTRA_PACKAGE_NAMES)
            ?: arrayListOf()
        currentIndex = savedInstanceState?.getInt(STATE_CURRENT_INDEX) ?: 0

        if (savedInstanceState == null) {
            uninstallNext()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(STATE_PACKAGE_NAMES, packageNames)
        outState.putInt(STATE_CURRENT_INDEX, currentIndex)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != UNINSTALL_REQUEST_CODE) return

        if (resultCode == RESULT_OK) {
            currentIndex += 1
            uninstallNext()
        } else {
            finish()
        }
    }

    private fun uninstallNext() {
        while (
            currentIndex < packageNames.size &&
            !isPackageInstalled(packageNames[currentIndex])
        ) {
            currentIndex += 1
        }

        val packageName = packageNames.getOrNull(currentIndex)
        if (packageName == null) {
            finish()
            return
        }

        val uninstallIntent = Intent(
            Intent.ACTION_UNINSTALL_PACKAGE,
            "package:$packageName".toUri(),
        ).putExtra(Intent.EXTRA_RETURN_RESULT, true)

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(uninstallIntent, UNINSTALL_REQUEST_CODE)
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error) { "Failed to uninstall extension $packageName" }
            currentIndex += 1
            uninstallNext()
        }
    }

    companion object {
        private const val EXTRA_PACKAGE_NAMES = "ExtensionUninstallActivity.packageNames"
        private const val STATE_PACKAGE_NAMES = "ExtensionUninstallActivity.state.packageNames"
        private const val STATE_CURRENT_INDEX = "ExtensionUninstallActivity.state.currentIndex"

        fun createIntent(context: Context, packageNames: List<String>): Intent {
            return Intent(context, ExtensionUninstallActivity::class.java)
                .putStringArrayListExtra(EXTRA_PACKAGE_NAMES, ArrayList(packageNames))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

private const val UNINSTALL_REQUEST_CODE = 501
