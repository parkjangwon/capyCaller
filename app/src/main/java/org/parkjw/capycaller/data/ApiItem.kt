package org.parkjw.capycaller.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 개별 API 요청에 대한 모든 정보를 담는 데이터 클래스입니다.
 * @Parcelize 어노테이션을 사용하여 이 객체를 Intent 등을 통해 다른 컴포넌트로 쉽게 전달할 수 있습니다.
 *
 * @property id API 아이템의 고유 식별자 (UUID).
 * @property name API의 이름 (예: "사용자 정보 가져오기").
 * @property url 요청을 보낼 URL 주소.
 * @property method HTTP 요청 메소드 (GET, POST, PUT, DELETE 등).
 * @property headers HTTP 요청 헤더 목록. 각 헤더는 Key-Value 쌍으로 구성됩니다.
 * @property queryParams URL에 추가될 쿼리 파라미터 목록. 각 파라미터는 Key-Value 쌍으로 구성됩니다.
 * @property body HTTP 요청 본문 (주로 POST, PUT 요청에 사용).
 * @property bodyType 요청 본문의 MIME 타입 (예: "application/json", "text/plain").
 * @property isShortcut 이 API를 앱 동적 바로가기에 추가할지 여부.
 * @property memo API에 대한 사용자 메모.
 */
@Parcelize
data class ApiItem(
    val id: String,
    val name: String,
    val url: String,
    val method: String,
    val headers: List<Pair<String, String>> = emptyList(),
    val queryParams: List<Pair<String, String>> = emptyList(),
    val body: String = "",
    val bodyType: String = "application/json",
    var isShortcut: Boolean = false,
    val memo: String = ""
) : Parcelable
