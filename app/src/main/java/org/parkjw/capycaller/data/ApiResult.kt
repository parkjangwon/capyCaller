package org.parkjw.capycaller.data

/**
 * API 호출의 결과를 나타내는 sealed class 입니다.
 * 호출이 성공했는지 또는 실패했는지에 따라 두 가지 상태(Success, Error) 중 하나를 가집니다.
 * Sealed class를 사용하면 when 표현식에서 모든 하위 클래스를 처리하도록 강제할 수 있어 안정적입니다.
 */
sealed class ApiResult {
    /**
     * API 호출이 성공했을 때의 데이터를 담는 클래스입니다.
     * @property code HTTP 응답 코드 (예: 200, 201).
     * @property data 응답 본문 데이터 (주로 JSON 또는 XML 형태의 문자열).
     * @property headers 응답 헤더 맵.
     * @property time API 호출에 소요된 시간 (밀리초 단위).
     */
    data class Success(
        val code: Int,
        val data: String,
        val headers: Map<String, String>,
        val time: Long
    ) : ApiResult()

    /**
     * API 호출이 실패했을 때의 정보를 담는 클래스입니다.
     * @property code HTTP 응답 코드 (예: 404, 500) 또는 네트워크 오류 시 0.
     * @property message 오류에 대한 설명 메시지.
     */
    data class Error(
        val code: Int,
        val message: String
    ) : ApiResult()
}
