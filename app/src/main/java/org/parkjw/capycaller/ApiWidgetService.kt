
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

/**
 * 위젯의 컬렉션(예: ListView)에 데이터를 제공하는 RemoteViewsService 입니다.
 */
class ApiWidgetService : RemoteViewsService() {
    /**
     * 시스템이 이 서비스에 바인딩될 때 호출되며, RemoteViewsFactory 객체를 반환해야 합니다.
     * 이 팩토리가 위젯의 각 아이템 뷰를 생성하고 데이터를 바인딩하는 역할을 합니다.
     * @param intent 이 서비스를 시작한 인텐트. 위젯 ID와 같은 정보를 포함할 수 있습니다.
     * @return 컬렉션 뷰를 채울 뷰 팩토리.
     */
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ApiWidgetRemoteViewsFactory(this.applicationContext, intent)
    }
}

/**
 * ApiWidgetService를 위한 RemoteViewsFactory 구현체입니다.
 * 위젯의 ListView에 표시될 각 아이템의 뷰를 생성하고 데이터를 관리합니다.
 */
class ApiWidgetRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    // 위젯에 표시될 API 아이템 목록
    private var apiItems: List<ApiItem> = emptyList()

    /**
     * 팩토리가 처음 생성될 때 한 번 호출됩니다. 초기 설정에 사용됩니다.
     */
    override fun onCreate() {
        // 특별한 초기화 작업이 필요 없으므로 비워둡니다.
    }

    /**
     * 데이터 세트가 변경되었을 때 호출됩니다. (예: notifyAppWidgetViewDataChanged() 호출 시)
     * 여기서 위젯에 표시할 데이터를 새로고침합니다.
     */
    override fun onDataSetChanged() {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val repository = ApiRepository(context)
        val allApis = repository.getApiItems() // 모든 API 목록 로드
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        // 현재 위젯 ID에 해당하는 설정에서 표시할 API ID 목록을 가져옵니다.
        val selectedIds = prefs.getString("widget_multi_$appWidgetId", null)?.split(",")?.toSet() ?: emptySet()
        // 선택된 ID에 해당하는 API만 필터링하여 목록을 업데이트합니다.
        apiItems = allApis.filter { it.id in selectedIds }
    }

    /**
     * 팩토리가 파괴될 때 호출됩니다. 리소스 해제에 사용됩니다.
     */
    override fun onDestroy() {
        // API 아이템 목록을 비웁니다.
        apiItems = emptyList()
    }

    /**
     * 데이터 세트의 아이템 개수를 반환합니다.
     */
    override fun getCount(): Int = apiItems.size

    /**
     * 지정된 위치(position)에 있는 아이템의 RemoteViews 객체를 반환합니다.
     * @param position 아이템의 위치.
     * @return 해당 위치의 아이템을 표시할 RemoteViews 객체.
     */
    override fun getViewAt(position: Int): RemoteViews {
        val apiItem = apiItems[position]
        // 각 아이템에 대한 레이아웃(widget_button)으로 RemoteViews를 생성합니다.
        val views = RemoteViews(context.packageName, R.layout.widget_button)
        // 버튼의 텍스트를 API 이름으로 설정합니다.
        views.setTextViewText(R.id.widget_button, apiItem.name)

        // 각 리스트 아이템에 대해 고유한 PendingIntent를 생성합니다.
        // 템플릿을 사용하는 것보다 더 직접적이고 안정적인 접근 방식입니다.
        val clickIntent = Intent(context, TransparentActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            // 클릭 시 호출할 API의 ID를 데이터로 전달합니다.
            data = Uri.parse("myapp://apicall/${apiItem.id}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // 각 아이템에 대한 PendingIntent가 고유하도록 요청 코드(apiItem.id.hashCode())와
        // 고유한 데이터 URI를 사용합니다.
        val pendingIntent = PendingIntent.getActivity(
            context,
            apiItem.id.hashCode(), // 각 아이템별 고유 요청 코드
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 생성된 PendingIntent를 버튼의 클릭 이벤트에 설정합니다.
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

        return views
    }

    /**
     * 데이터가 로드되는 동안 표시할 로딩 뷰를 반환합니다. null을 반환하면 기본 로딩 뷰가 사용됩니다.
     */
    override fun getLoadingView(): RemoteViews? = null

    /**
     * 데이터 세트에서 사용되는 뷰 유형의 개수를 반환합니다. 여기서는 모든 아이템이 동일한 뷰를 사용하므로 1입니다.
     */
    override fun getViewTypeCount(): Int = 1

    /**
     * 지정된 위치에 있는 아이템의 고유 ID를 반환합니다.
     */
    override fun getItemId(position: Int): Long = apiItems[position].id.hashCode().toLong()

    /**
     * 아이템 ID가 데이터 변경에도 불구하고 안정적으로 유지되는지 여부를 반환합니다.
     * true를 반환하면 시스템이 최적화를 수행할 수 있습니다.
     */
    override fun hasStableIds(): Boolean = true
}
