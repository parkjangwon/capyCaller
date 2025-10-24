package org.parkjw.capycaller

import android.app.Application
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiRepository

/**
 * API 목록 데이터를 관리하고 UI와 데이터 소스 간의 상호작용을 처리하는 ViewModel입니다.
 * AndroidViewModel을 상속받아 Application 컨텍스트에 접근할 수 있습니다.
 */
class ApiViewModel(application: Application) : AndroidViewModel(application) {

    // 데이터 저장을 담당하는 리포지토리
    private val repository = ApiRepository(application)
    // 앱 바로가기 생성을 관리하는 컨트롤러
    private val shortcutController = ShortcutController(application)

    // API 아이템 목록을 관리하는 MutableStateFlow (내부적으로 사용)
    private val _apiItems = MutableStateFlow<List<ApiItem>>(emptyList())
    // 외부(UI)에 노출되는 API 아이템 목록 (읽기 전용)
    val apiItems: StateFlow<List<ApiItem>> = _apiItems.asStateFlow()

    init {
        // ViewModel 초기화 시 API 목록을 불러옵니다.
        loadApis()
    }

    /**
     * 리포지토리에서 API 목록을 비동기적으로 불러와 StateFlow를 업데이트하고, 앱 바로가기를 갱신합니다.
     */
    private fun loadApis() {
        viewModelScope.launch {
            val apiItems = repository.getApiItems()
            _apiItems.value = apiItems
            shortcutController.updateShortcuts(apiItems)
        }
    }

    /**
     * 주어진 ID에 해당하는 ApiItem을 반환합니다.
     * @param id 찾고자 하는 ApiItem의 ID.
     * @return ID에 해당하는 ApiItem 객체. 없으면 null.
     */
    fun getApiItem(id: String?): ApiItem? {
        return _apiItems.value.find { it.id == id }
    }

    /**
     * 새로운 API 아이템을 추가합니다.
     * 바로가기 옵션이 켜져 있을 경우, 최대 바로가기 개수를 확인합니다.
     * @param apiItem 추가할 ApiItem 객체.
     */
    fun addApi(apiItem: ApiItem) {
        viewModelScope.launch {
            var itemToSave = apiItem
            // 바로가기 생성을 요청한 경우
            if (apiItem.isShortcut) {
                val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(getApplication())
                val currentShortcutCount = _apiItems.value.count { it.isShortcut }
                // 현재 바로가기 개수가 최대치에 도달했는지 확인
                if (currentShortcutCount >= maxShortcuts) {
                    itemToSave = apiItem.copy(isShortcut = false) // 바로가기 옵션을 끈 상태로 저장
                    Toast.makeText(getApplication(), "바로가기 최대 개수에 도달했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            val updatedList = _apiItems.value + itemToSave
            repository.saveApiItems(updatedList) // 리포지토리에 저장
            _apiItems.value = updatedList // StateFlow 업데이트
            shortcutController.updateShortcuts(updatedList) // 바로가기 업데이트
        }
    }

    /**
     * 기존 API 아이템을 수정합니다.
     * 바로가기 옵션이 새로 켜진 경우, 최대 바로가기 개수를 확인합니다.
     * @param apiItem 수정할 ApiItem 객체.
     */
    fun updateApi(apiItem: ApiItem) {
        viewModelScope.launch {
            var itemToSave = apiItem
            val originalItem = _apiItems.value.find { it.id == apiItem.id }
            // 바로가기 옵션이 새로 활성화된 경우
            if (apiItem.isShortcut && originalItem?.isShortcut == false) {
                val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(getApplication())
                val currentShortcutCount = _apiItems.value.count { it.isShortcut }
                // 현재 바로가기 개수가 최대치에 도달했는지 확인
                if (currentShortcutCount >= maxShortcuts) {
                    itemToSave = apiItem.copy(isShortcut = false) // 바로가기 옵션을 끈 상태로 저장
                    Toast.makeText(getApplication(), "바로가기 최대 개수에 도달했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            val updatedList = _apiItems.value.map {
                if (it.id == itemToSave.id) itemToSave else it // ID가 일치하는 아이템을 교체
            }
            repository.saveApiItems(updatedList) // 리포지토리에 저장
            _apiItems.value = updatedList // StateFlow 업데이트
            shortcutController.updateShortcuts(updatedList) // 바로가기 업데이트
        }
    }

    /**
     * 특정 API 아이템을 삭제합니다.
     * @param apiItem 삭제할 ApiItem 객체.
     */
    fun deleteApi(apiItem: ApiItem) {
        viewModelScope.launch {
            val updatedList = _apiItems.value.filter { it.id != apiItem.id } // 해당 아이템을 제외한 새 리스트 생성
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    /**
     * 여러 개의 API 아이템을 한 번에 삭제합니다.
     * @param apiItems 삭제할 ApiItem 객체 리스트.
     */
    fun deleteApis(apiItems: List<ApiItem>) {
        viewModelScope.launch {
            val idsToDelete = apiItems.map { it.id }.toSet() // 삭제할 ID 집합 생성
            val updatedList = _apiItems.value.filter { it.id !in idsToDelete } // 해당 아이템들을 제외한 새 리스트 생성
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    /**
     * 특정 API 아이템을 복사하여 새 아이템으로 추가합니다.
     * 복사된 아이템의 이름은 "[원본이름] Copy" 형식이며, 중복 시 숫자를 붙입니다.
     * @param apiItem 복사할 ApiItem 객체.
     */
    fun copyApi(apiItem: ApiItem) {
        viewModelScope.launch {
            var newName = "${apiItem.name} Copy"
            var counter = 2
            // 중복되지 않는 새 이름을 찾습니다.
            while (_apiItems.value.any { it.name == newName }) {
                newName = "${apiItem.name} Copy $counter"
                counter++
            }

            val newApiItem = apiItem.copy(
                id = java.util.UUID.randomUUID().toString(), // 새 ID 발급
                name = newName, // 새 이름 설정
                isShortcut = false // 복사된 아이템은 바로가기 비활성화
            )

            val updatedList = _apiItems.value + newApiItem
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    /**
     * 백업 데이터로 API 목록을 복원합니다.
     * @param restoredApis 복원할 ApiItem 객체 리스트.
     */
    fun restoreApis(restoredApis: List<ApiItem>) {
        viewModelScope.launch {
            repository.saveApiItems(restoredApis)
            _apiItems.value = restoredApis
            shortcutController.updateShortcuts(restoredApis)
        }
    }
}
