package com.jarrlyyy.guessthenumber.data.repository

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_RESULT = "com.jarrlyyy.guessthenumber.UPDATE_INSTALL_RESULT"
        private const val TAG = "GTNUpdateInstaller"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESULT) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty().ifBlank { null }

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = getConfirmationIntent(intent)
                if (confirmationIntent == null) {
                    Log.e(TAG, "PackageInstaller requested user confirmation but supplied no confirmation Intent.")
                    Toast.makeText(
                        context,
                        "Android needs you to confirm the update. Open the app and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                try {
                    confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmationIntent)
                } catch (error: Exception) {
                    Log.e(TAG, "Could not open PackageInstaller confirmation screen.", error)
                    Toast.makeText(
                        context,
                        "Android needs permission to install the update. Open the app and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "PackageInstaller completed the update successfully.")
                Toast.makeText(
                    context,
                    "Update installed. Restart GuessTheNumber to use it.",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                val legacyStatus = intent.getIntExtra(PackageInstaller.EXTRA_LEGACY_STATUS, Int.MIN_VALUE)
                val reason = message ?: "No status message supplied by Android"
                Log.e(
                    TAG,
                    "PackageInstaller update failed: status=$status, legacyStatus=$legacyStatus, message=$reason"
                )
                Toast.makeText(
                    context,
                    "Update failed (status $status): $reason",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getConfirmationIntent(resultIntent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resultIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            resultIntent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
    }
}
