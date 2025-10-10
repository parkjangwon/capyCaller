
package org.parkjw.capycaller

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.parkjw.capycaller.data.ApiRepository

class ApiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            prefs.remove("widget_multi_$it")
        }
        prefs.apply()
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val selectedIds = prefs.getString("widget_multi_$appWidgetId", null)?.split(",")?.toSet() ?: emptySet()

            val repository = ApiRepository(context)
            val allApis = repository.getApiItems()
            val selectedApis = allApis.filter { it.id in selectedIds }

            val views = RemoteViews(context.packageName, R.layout.api_widget)
            views.removeAllViews(R.id.widget_container)

            selectedApis.forEach { apiItem ->
                val button = RemoteViews(context.packageName, R.layout.widget_button)
                button.setTextViewText(R.id.widget_button, apiItem.name)

                val intent = Intent(context, TransparentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("myapp://apicall/${apiItem.id}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    apiItem.id.hashCode(), 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                button.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

                views.addView(R.id.widget_container, button)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
