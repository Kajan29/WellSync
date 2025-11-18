package com.example.wellsync.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wellsync.R
import com.example.wellsync.activities.MainActivity
import java.util.*

class NotificationUtils(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "wellsync_notifications"
        const val MORNING_NOTIFICATION_ID = 1001
        const val NIGHT_NOTIFICATION_ID = 1002
        const val HYDRATION_NOTIFICATION_ID = 1003
        const val WATER_ALERT_NOTIFICATION_ID = 1004
        const val WATER_REMINDER_30MIN_ID = 1005
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WellSync Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for WellSync wellness reminders"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleMorningNotification(hour: Int, minute: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "morning")
        }
        scheduleNotification(intent, hour, minute, MORNING_NOTIFICATION_ID)
    }

    fun scheduleNightNotification(hour: Int, minute: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "night")
        }
        scheduleNotification(intent, hour, minute, NIGHT_NOTIFICATION_ID)
    }

    fun scheduleHydrationReminder(intervalMinutes: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "hydration")
            putExtra("interval", intervalMinutes)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, HYDRATION_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = intervalMinutes * 60 * 1000L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        // Can't schedule exact alarms; fall back to inexact but allowedWhileIdle
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + intervalMillis,
                            pendingIntent
                        )
                        return
                    }
                } catch (_: Exception) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + intervalMillis,
                        pendingIntent
                    )
                    return
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            )
        }
    }

    fun scheduleWaterReminderAlert(delayMinutes: Int = 30) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "water_alert")
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, WATER_REMINDER_30MIN_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val delayMillis = delayMinutes * 60 * 1000L
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent
            )
        }
    }

    fun cancelWaterReminderAlert() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "water_alert")
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, WATER_REMINDER_30MIN_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelHydrationReminder() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("type", "hydration")
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, HYDRATION_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showWaterAlertNotification() {
        val dataManager = DataManager(context)
        val currentUser = dataManager.getCurrentUser() ?: return
        val currentIntake = dataManager.getWaterIntakeToday(currentUser)
        val goal = dataManager.getWaterGoal(currentUser)

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ Water Reminder Alert!")
            .setContentText("You haven't added water in 30 minutes. Stay hydrated! ($currentIntake ml / $goal ml)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Don't forget to drink water! You've had $currentIntake ml out of $goal ml today. Tap to add water intake."))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(WATER_ALERT_NOTIFICATION_ID, notification)
    }

    private fun scheduleNotification(intent: Intent, hour: Int, minute: Int, requestCode: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val type = intent.getStringExtra("type")
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mainIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val (title, message, notificationId) = when (type) {
                "morning" -> Triple(
                    "🌅 Good Morning!",
                    "Ready to start your wellness journey today?",
                    MORNING_NOTIFICATION_ID
                )
                "night" -> Triple(
                    "🌙 Evening Reflection",
                    "How was your wellness journey today?",
                    NIGHT_NOTIFICATION_ID
                )
                "hydration" -> {
                    val dataManager = DataManager(context)
                    val currentUser = dataManager.getCurrentUser()
                    val hasMetGoal = if (currentUser != null) {
                        val currentIntake = dataManager.getWaterIntakeToday(currentUser)
                        val goal = dataManager.getWaterGoal(currentUser)
                        currentIntake >= goal
                    } else {
                        false
                    }

                    if (hasMetGoal) {
                        val notificationUtils = NotificationUtils(context)
                        notificationUtils.cancelHydrationReminder()
                        return
                    }

                    val intervalMinutes = intent.getIntExtra("interval", 60)
                    val notificationUtils = NotificationUtils(context)
                    notificationUtils.scheduleHydrationReminder(intervalMinutes)

                    notificationUtils.scheduleWaterReminderAlert(30)

                    Triple(
                        "💧 Hydration Time!",
                        "Time to drink a glass of water",
                        HYDRATION_NOTIFICATION_ID
                    )
                }
                "water_alert" -> {
                    val dataManager = DataManager(context)
                    val currentUser = dataManager.getCurrentUser()
                    if (currentUser == null) return

                    if (dataManager.hasRecentWaterIntake(currentUser, withinMinutes = 30)) {
                        val notificationUtils = NotificationUtils(context)
                        notificationUtils.cancelWaterReminderAlert()
                        return
                    }

                    val notificationUtils = NotificationUtils(context)
                    notificationUtils.showWaterAlertNotification()
                    return
                }
                else -> return
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(notificationId, notification)
        }
    }
}
