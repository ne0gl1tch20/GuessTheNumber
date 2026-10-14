package com.jarrlyyy.guessthenumber.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {
    companion object { const val ACTION_RESULT = "com.jarrlyyy.guessthenumber.UPDATE_INSTALL_RESULT" }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESULT) return
        val status = intent.getIntExtra("android.content.pm.extra.STATUS", -1)
        val message = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE")
        val text = if (status == 0) "Update installed. Restart GuessTheNumber to use it." else "Update failed: ${message ?: "unknown error"}"
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}
