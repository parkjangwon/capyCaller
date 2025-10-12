package org.parkjw.capycaller.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * API 데이터에 대한 데이터 액세스를 관리하는 리포지토리 클래스입니다.
 * SharedPreferences를 사용하여 API 목록을 로컬에 저장하고 불러오는 역할을 합니다.
 * @param context SharedPreferences 인스턴스를 얻기 위한 컨텍스트.
 */
class ApiRepository(context: Context) {

    // "api_prefs"라는 이름으로 SharedPreferences 파일을 가져옵니다. 앱 내에서만 접근 가능(MODE_PRIVATE)합니다.
    private val sharedPreferences = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)
    // JSON 직렬화 및 역직렬화를 위한 Gson 라이브러리 인스턴스입니다.
    private val gson = Gson()

    /**
     * SharedPreferences에 저장된 모든 API 아이템 목록을 불러옵니다.
     * @return 저장된 ApiItem 객체의 리스트. 저장된 데이터가 없으면 빈 리스트를 반환합니다.
     */
    fun getApiItems(): List<ApiItem> {
        // "api_items" 키로 저장된 JSON 문자열을 가져옵니다. 데이터가 없으면 null을 반환합니다.
        val json = sharedPreferences.getString("api_items", null)
        return if (json != null) {
            // JSON 문자열이 있는 경우, Gson을 사용하여 List<ApiItem> 타입으로 역직렬화합니다.
            // TypeToken을 사용하여 제네릭 타입(List<ApiItem>) 정보를 Gson에 전달합니다.
            val type = object : TypeToken<List<ApiItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            // JSON 문자열이 없는 경우, 빈 리스트를 반환합니다.
            emptyList()
        }
    }

    /**
     * 주어진 API 아이템 목록을 SharedPreferences에 JSON 형태로 저장합니다.
     * 기존에 저장된 데이터는 덮어씌워집니다.
     * @param apiItems 저장할 ApiItem 객체의 리스트.
     */
    fun saveApiItems(apiItems: List<ApiItem>) {
        // ApiItem 리스트를 JSON 문자열로 직렬화합니다.
        val json = gson.toJson(apiItems)
        // SharedPreferences.edit()를 사용하여 데이터를 저장합니다.
        // Kotlin 확장 함수(edit)를 사용하여 코드를 간소화합니다.
        sharedPreferences.edit {
            putString("api_items", json)
        }
    }
}
