
package org.parkjw.capycaller

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiRepository

class ApiWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ApiWidgetRemoteViewsFactory(this.applicationContext, intent)
    }
}

class ApiWidgetRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var apiItems: List<ApiItem> = emptyList()

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val repository = ApiRepository(context)
        val allApis = repository.getApiItems()
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val selectedIds = prefs.getString("widget_multi_$appWidgetId", null)?.split(",")?.toSet() ?: emptySet()
        apiItems = allApis.filter { it.id in selectedIds }
    }

    override fun onDestroy() {
        apiItems = emptyList()
    }

    override fun getCount(): Int = apiItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val apiItem = apiItems[position]
        val views = RemoteViews(context.packageName, R.layout.widget_button)
        views.setTextViewText(R.id.widget_button, apiItem.name)

        // Create a unique PendingIntent for each list item.
        // This is a more direct and robust approach than using a template.
        val clickIntent = Intent(context, ApiWidgetProvider::class.java).apply {
            action = "org.parkjw.capycaller.ACTION_API_CLICK"
            putExtra("api_id", apiItem.id)
            // Add a unique data URI to ensure each PendingIntent is distinct.
            data = Uri.parse("widget://item/${apiItem.id}")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            apiItem.id.hashCode(), // Unique request code for each item
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = apiItems[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}
