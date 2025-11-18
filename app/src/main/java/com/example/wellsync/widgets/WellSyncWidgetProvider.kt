package com.example.wellsync.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.wellsync.R
import com.example.wellsync.activities.LoginActivity
import com.example.wellsync.activities.MainActivity
import com.example.wellsync.utils.DataManager

class WellSyncWidgetProvider : AppWidgetProvider() {

    companion object {
        const val UPDATE_WIDGET_ACTION = "com.example.wellsync.UPDATE_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.wellsync_widget_layout)

        try {
            val dataManager = DataManager(context)

            // Get current user
            val currentUser = dataManager.getCurrentUser()

            if (currentUser != null) {
                // Get pending habits count
                val pendingHabits = dataManager.getPendingHabitsCount()
                views.setTextViewText(R.id.tv_pending_habits, "$pendingHabits")

                // Get water intake progress
                val waterProgress = dataManager.getWaterIntakeProgress()
                val waterGoal = dataManager.getWaterGoal()
                views.setTextViewText(R.id.tv_water_progress, "$waterProgress/$waterGoal ml")
                views.setProgressBar(R.id.progress_water, waterGoal, waterProgress, false)

                // Get step count
                val stepCount = dataManager.getStepsToday(currentUser)
                val stepGoal = dataManager.getStepGoal()
                views.setTextViewText(R.id.tv_step_count, "$stepCount")
                views.setTextViewText(R.id.tv_step_goal, "/ $stepGoal")
                views.setProgressBar(R.id.progress_steps, stepGoal, stepCount, false)

                // Set click intent to open main app when logged in
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            } else {
                // Show default values when no user is logged in
                setDefaultWidgetValues(views, context)
            }
        } catch (e: Exception) {
            Log.e("WellSyncWidget", "Error updating widget: ${e.message}", e)
            // If DataManager fails, show default values
            setDefaultWidgetValues(views, context)
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setDefaultWidgetValues(views: RemoteViews, context: Context) {
        views.setTextViewText(R.id.tv_pending_habits, "0")
        views.setTextViewText(R.id.tv_water_progress, "0/2000 ml")
        views.setTextViewText(R.id.tv_step_count, "0")
        views.setTextViewText(R.id.tv_step_goal, "/ 10000")
        views.setProgressBar(R.id.progress_water, 2000, 0, false)
        views.setProgressBar(R.id.progress_steps, 10000, 0, false)

        // Set click intent to open login
        val intent = Intent(context, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == UPDATE_WIDGET_ACTION) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, WellSyncWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
