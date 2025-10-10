
package org.parkjw.capycaller

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class SingleApiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        appWidgetIds.forEach { appWidgetId ->
            val apiId = prefs.getString("widget_${appWidgetId}_id", null)
            val apiName = prefs.getString("widget_${appWidgetId}_name", "Tap to setup")
            val views = RemoteViews(context.packageName, R.layout.single_api_widget)
            views.setTextViewText(R.id.widget_button, apiName)

            val intent = if (apiId != null) {
                Intent(context, TransparentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("myapp://apicall/$apiId")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            } else {
                Intent(context, SingleApiWidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
            }
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            prefs.remove("widget_${it}_id")
            prefs.remove("widget_${it}_name")
        }
        prefs.apply()
    }
}
