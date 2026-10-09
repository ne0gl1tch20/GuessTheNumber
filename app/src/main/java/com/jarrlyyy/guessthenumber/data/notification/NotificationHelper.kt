package com.jarrlyyy.guessthenumber.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jarrlyyy.guessthenumber.MainActivity
import com.jarrlyyy.guessthenumber.R
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager
import com.jarrlyyy.guessthenumber.data.store.SaveManager
import org.json.JSONObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object NotificationHelper {
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    const val CHANNEL_ID = "game_reminder_channel"
    const val PROGRESSION_CHANNEL_ID = "progression_channel"
    const val EVENT_CHANNEL_ID = "event_channel"
    const val NOTIFICATION_ID = 1001
    const val PROGRESSION_NOTIFICATION_ID = 1002
    const val EVENT_NOTIFICATION_ID = 1003

    enum class Type(val channelId: String, val id: Int, val titleKey: String, val bodyKey: String, val fallbackTitle: String, val fallbackBody: String, val destination: String, val preferenceKey: String) {
        REMINDER(CHANNEL_ID, NOTIFICATION_ID, "notification_reminder_title", "notification_reminder_body", "Guess The Number", "Your numbers miss you! Come back and guess the number!", "play", "notificationRemindersEnabled"),
        PROGRESSION(PROGRESSION_CHANNEL_ID, PROGRESSION_NOTIFICATION_ID, "notification_progression_title", "notification_progression_body", "Progression milestone", "A new progression milestone is ready.", "world_map", "notificationProgressionEnabled"),
        EVENT(EVENT_CHANNEL_ID, EVENT_NOTIFICATION_ID, "notification_event_title", "notification_event_body", "A game event is waiting", "A limited game event is ready.", "play", "notificationEventsEnabled")
    }

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val locale = createLocaleManager(context)
        listOf(
            NotificationChannel(CHANNEL_ID, locale.getString("notification_channel_reminders", "Game Reminders"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = locale.getString("notification_channel_reminders_desc", "Optional reminders to return to Guess The Number")
            },
            NotificationChannel(PROGRESSION_CHANNEL_ID, locale.getString("notification_channel_progression", "Progression"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = locale.getString("notification_channel_progression_desc", "Optional progression milestone notifications")
            },
            NotificationChannel(EVENT_CHANNEL_ID, locale.getString("notification_channel_events", "Events"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = locale.getString("notification_channel_events_desc", "Optional event notifications")
            }
        ).forEach(manager::createNotificationChannel)
    }

    fun createNotificationChannel(context: Context) = createNotificationChannels(context)

    fun show(context: Context, type: Type) {
        val appContext = context.applicationContext
        notificationScope.launch {
            if (!isCategoryEnabled(appContext, type)) return@launch
            createNotificationChannels(appContext)
            val locale = createLocaleManager(appContext)
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_destination", type.destination)
            }
            val pendingIntent = PendingIntent.getActivity(appContext, type.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = NotificationCompat.Builder(appContext, type.channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(locale.getString(type.titleKey, type.fallbackTitle))
                .setContentText(locale.getString(type.bodyKey, type.fallbackBody))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            try {
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(type.id, notification)
            } catch (_: SecurityException) {
                // Android 13+ notification permission may be denied.
            }
        }
    }

    private fun isCategoryEnabled(context: Context, type: Type): Boolean = runCatching {
        val settings = JSONObject(runBlocking { SaveManager(context).getAppPreferencesJson() })
        settings.optBoolean("notificationsEnabled", true) && settings.optBoolean(type.preferenceKey, true)
    }.getOrDefault(false)

    private fun createLocaleManager(context: Context): LocaleManager {
        val locale = LocaleManager(context)
        val tag = runCatching {
            val raw = runBlocking { SaveManager(context).getAppPreferencesJson() }
            JSONObject(raw).optString("locale", "en-US")
        }.getOrDefault("en-US")
        runBlocking { locale.loadLocaleForTag(tag) }
        return locale
    }

    fun showReminderNotification(context: Context) = show(context, Type.REMINDER)
    fun showProgressionNotification(context: Context) = show(context, Type.PROGRESSION)
    fun showEventNotification(context: Context) = show(context, Type.EVENT)
}
