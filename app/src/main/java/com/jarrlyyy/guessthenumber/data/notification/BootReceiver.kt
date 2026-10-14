package com.jarrlyyy.guessthenumber.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = runCatching {
                    JSONObject(SaveManager(appContext).getAppPreferencesJson())
                }.getOrElse { JSONObject() }
                val notificationsEnabled = preferences.optBoolean("notificationsEnabled", true)
                val remindersEnabled = preferences.optBoolean("notificationRemindersEnabled", true)
                val enabled = notificationsEnabled && remindersEnabled
                val intervalHours = preferences.optLong("notificationIntervalHours", 24L).coerceIn(12L, 48L)
                val workManager = WorkManager.getInstance(appContext)
                if (!notificationsEnabled) {
                    NotificationHelper.Type.values().forEach { NotificationHelper.cancel(appContext, it) }
                } else if (!remindersEnabled) {
                    NotificationHelper.cancel(appContext, NotificationHelper.Type.REMINDER)
                }
                if (!enabled) {
                    workManager.cancelUniqueWork(GameReminderWorker.WORK_NAME)
                } else {
                    val request = PeriodicWorkRequestBuilder<GameReminderWorker>(
                        intervalHours, TimeUnit.HOURS
                    ).build()
                    workManager.enqueueUniquePeriodicWork(
                        GameReminderWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
