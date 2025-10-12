
package org.parkjw.capycaller

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.parkjw.capycaller.data.ApiRepository

/**
 * 홈 화면에 표시되는 다중 API 호출 위젯을 관리하는 클래스입니다.
 * AppWidgetProvider를 상속받아 위젯의 생명주기 이벤트를 처리합니다.
 */
class ApiWidgetProvider : AppWidgetProvider() {

    /**
     * 위젯이 업데이트되어야 할 때 호출됩니다. (예: 위젯 추가 시, 일정 시간 경과 시)
     * @param context 컨텍스트
     * @param appWidgetManager 위젯 관리자
     * @param appWidgetIds 업데이트할 위젯 ID 배열
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 각 위젯 인스턴스에 대해 업데이트를 수행합니다.
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * 위젯이 삭제될 때 호출됩니다.
     * 해당 위젯과 관련된 설정(SharedPreferences)을 삭제합니다.
     * @param context 컨텍스트
     * @param appWidgetIds 삭제된 위젯 ID 배열
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            // 삭제된 위젯 ID에 해당하는 설정 값을 제거합니다.
            prefs.remove("widget_multi_$it")
        }
        prefs.apply()
    }

    companion object {
        /**
         * 특정 위젯 인스턴스의 UI를 업데이트합니다.
         * @param context 컨텍스트
         * @param appWidgetManager 위젯 관리자
         * @param appWidgetId 업데이트할 위젯 ID
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // 위젯 설정을 저장하는 SharedPreferences를 가져옵니다.
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            // 이 위젯에 표시하도록 선택된 API들의 ID 목록을 불러옵니다.
            val selectedIds = prefs.getString("widget_multi_$appWidgetId", null)?.split(",")?.toSet() ?: emptySet()

            // API 데이터를 가져오기 위해 리포지토리를 생성합니다.
            val repository = ApiRepository(context)
            // 저장된 모든 API 목록을 가져옵니다.
            val allApis = repository.getApiItems()
            // 선택된 ID에 해당하는 API들만 필터링합니다.
            val selectedApis = allApis.filter { it.id in selectedIds }

            // 위젯의 기본 레이아웃에 대한 RemoteViews 객체를 생성합니다.
            val views = RemoteViews(context.packageName, R.layout.api_widget)
            // 기존에 추가된 버튼들을 모두 제거하여 UI를 초기화합니다.
            views.removeAllViews(R.id.widget_container)

            // 선택된 각 API에 대해 버튼을 생성하고 위젯에 추가합니다.
            selectedApis.forEach { apiItem ->
                // 개별 API 버튼에 대한 RemoteViews 객체를 생성합니다.
                val button = RemoteViews(context.packageName, R.layout.widget_button)
                // 버튼의 텍스트를 API 이름으로 설정합니다.
                button.setTextViewText(R.id.widget_button, apiItem.name)

                // 버튼 클릭 시 API 호출을 처리할 TransparentActivity를 실행할 인텐트를 생성합니다.
                val intent = Intent(context, TransparentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    // 인텐트에 API ID를 포함하는 커스텀 URI를 데이터로 설정하여 어떤 API를 호출할지 전달합니다.
                    data = Uri.parse("myapp://apicall/${apiItem.id}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                // 인텐트를 즉시 실행하지 않고, 특정 시점(버튼 클릭)에 실행되도록 PendingIntent를 생성합니다.
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    apiItem.id.hashCode(), // 각 PendingIntent를 구분하기 위한 고유한 요청 코드
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // 버튼 클릭 시 생성된 PendingIntent가 실행되도록 설정합니다.
                button.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

                // 생성된 버튼을 위젯의 컨테이너에 추가합니다.
                views.addView(R.id.widget_container, button)
            }
            // AppWidgetManager를 통해 위젯의 UI를 최종적으로 업데이트합니다.
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
