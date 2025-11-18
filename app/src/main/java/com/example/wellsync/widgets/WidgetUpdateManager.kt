package com.example.wellsync.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class WidgetUpdateManager(private val context: Context) {

    fun updateAllWidgets() {
        val intent = Intent(context, WellSyncWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, WellSyncWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        context.sendBroadcast(intent)
    }

    companion object {
        fun updateWidgets(context: Context) {
            val updateManager = WidgetUpdateManager(context)
            updateManager.updateAllWidgets()
        }
    }
}
