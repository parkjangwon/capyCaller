package org.parkjw.capycaller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 앱 전체에서 알림(Notification) 생성을 도와주는 헬퍼 객체입니다.
 * 싱글톤(object)으로 구현되어 어디서든 쉽게 접근하여 사용할 수 있습니다.
 */
object NotificationHelper {

    // 알림 채널을 식별하기 위한 고유 ID
    private const val CHANNEL_ID = "api_execution_channel"
    // 사용자에게 표시될 알림 채널의 이름
    private const val CHANNEL_NAME = "API 실행"

    /**
     * Android 8.0 (Oreo, API 26) 이상 버전에서 알림을 표시하기 위해 필요한 알림 채널을 생성합니다.
     * Oreo 이전 버전에서는 채널이 필요 없으므로 이 코드는 무시됩니다.
     * @param context NotificationManager를 얻기 위한 컨텍스트.
     */
    fun createNotificationChannel(context: Context) {
        // Build.VERSION_CODES.O 이상에서만 채널을 생성합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // 알림의 중요도 설정 (기본값)
            ).apply {
                description = "API 실행 상태에 대한 알림" // 채널에 대한 설명 (사용자 설정에 표시됨)
            }
            // 시스템 서비스로부터 NotificationManager 인스턴스를 가져옵니다.
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 생성한 채널을 시스템에 등록합니다.
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 사용자에게 상태 알림을 표시합니다.
     * @param context 알림을 생성하고 표시하기 위한 컨텍스트.
     * @param title 알림의 제목.
     * @param content 알림의 내용.
     */
    fun showNotification(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationCompat.Builder를 사용하여 알림을 구성합니다.
        // 호환성을 위해 v4 라이브러리의 Compat 클래스를 사용하는 것이 좋습니다.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // 리소스 관련 문제를 방지하기 위해 시스템이 보장하는 아이콘을 사용합니다.
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 작은 아이콘 설정
            .setContentTitle(title) // 제목 설정
            .setContentText(content) // 내용 설정
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 중요도 설정 (Oreo 이전 버전을 위함)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // 긴 텍스트를 모두 표시할 수 있는 스타일 적용
            .setAutoCancel(true) // 사용자가 알림을 탭하면 자동으로 사라지도록 설정
            .build()

        // 각 알림이 이전 알림을 덮어쓰지 않고 새로 표시되도록 현재 시간을 고유 ID로 사용합니다.
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
