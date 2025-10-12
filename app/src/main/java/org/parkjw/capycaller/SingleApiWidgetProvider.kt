
package org.parkjw.capycaller

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/**
 * 단일 API 호출 위젯의 동작을 정의하는 클래스입니다.
 * AppWidgetProvider를 상속받아 위젯의 업데이트, 삭제 등 생명주기 이벤트를 처리합니다.
 */
class SingleApiWidgetProvider : AppWidgetProvider() {

    /**
     * 위젯이 업데이트되어야 할 때 호출됩니다. (예: 위젯 추가 시, 주기적 업데이트 시)
     * @param context 컨텍스트
     * @param appWidgetManager 위젯 관리자
     * @param appWidgetIds 업데이트할 위젯 ID 배열
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // 위젯 설정을 저장하는 SharedPreferences를 가져옵니다.
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        // 업데이트가 필요한 모든 위젯 인스턴스에 대해 반복합니다.
        appWidgetIds.forEach { appWidgetId ->
            // SharedPreferences에서 해당 위젯 ID에 저장된 API ID와 이름을 불러옵니다.
            val apiId = prefs.getString("widget_${appWidgetId}_id", null)
            val apiName = prefs.getString("widget_${appWidgetId}_name", "탭하여 설정")

            // 위젯의 레이아웃을 위한 RemoteViews 객체를 생성합니다.
            val views = RemoteViews(context.packageName, R.layout.single_api_widget)
            // 위젯 버튼의 텍스트를 불러온 API 이름으로 설정합니다.
            views.setTextViewText(R.id.widget_button, apiName)

            // 위젯 클릭 시 실행될 인텐트를 결정합니다.
            val intent = if (apiId != null) {
                // API가 설정된 경우: API 호출을 위한 TransparentActivity를 실행하는 인텐트 생성
                Intent(context, TransparentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("myapp://apicall/$apiId") // 호출할 API ID를 데이터로 전달
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            } else {
                // API가 설정되지 않은 경우: 위젯 설정 액티비티를 실행하는 인텐트 생성
                Intent(context, SingleApiWidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) // 설정할 위젯 ID 전달
                }
            }
            // 인텐트를 실행할 PendingIntent를 생성합니다.
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            // 생성된 PendingIntent를 위젯 버튼의 클릭 이벤트에 설정합니다.
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            
            // AppWidgetManager를 통해 위젯의 UI를 최종적으로 업데이트합니다.
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
            // 삭제된 위젯 ID에 해당하는 설정 값(API ID, 이름)을 제거합니다.
            prefs.remove("widget_${it}_id")
            prefs.remove("widget_${it}_name")
        }
        prefs.apply()
    }
}
