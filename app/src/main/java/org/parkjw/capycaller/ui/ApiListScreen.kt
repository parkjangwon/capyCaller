package org.parkjw.capycaller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.ui.theme.getHttpMethodColor

/**
 * 등록된 API 목록을 보여주는 메인 화면의 Composable 함수입니다.
 * 다중 선택 모드를 지원하여 여러 API를 한 번에 실행하거나 삭제할 수 있습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiListScreen(
    apiItems: List<ApiItem>,          // 표시할 API 아이템 목록
    onAddApi: () -> Unit,             // API 추가 버튼 클릭 콜백
    onApiClick: (ApiItem) -> Unit,    // API 아이템 클릭 콜백 (상세 화면 이동)
    onExecuteApis: (List<ApiItem>) -> Unit, // API 실행 콜백
    onDeleteApis: (List<ApiItem>) -> Unit,  // API 삭제 콜백
    onCopyApi: (ApiItem) -> Unit,       // API 복사 콜백
    onSettingsClick: () -> Unit,      // 설정 버튼 클릭 콜백
) {
    // 다중 선택 모드에서 선택된 API들의 ID를 저장하는 상태
    var selectedApiIds by remember { mutableStateOf(emptySet<String>()) }
    // 선택된 API가 하나 이상 있는지, 즉 다중 선택 모드인지 여부
    val isInSelectionMode = selectedApiIds.isNotEmpty()
    // 확인 대화상자(AlertDialog)를 표시할지 여부
    var showConfirmDialog by remember { mutableStateOf(false) }
    // 확인 대화상자에서 "확인" 버튼을 눌렀을 때 실행될 액션
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    // 확인 대화상자의 제목
    var confirmDialogTitle by remember { mutableStateOf("") }
    // 확인 대화상자의 내용
    var confirmDialogText by remember { mutableStateOf("") }

    // 확인 대화상자 Composable
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(confirmDialogTitle) },
            text = { Text(confirmDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction() // 설정된 액션 실행
                        showConfirmDialog = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 다중 선택 모드일 때 뒤로가기 버튼을 누르면 선택 모드를 해제합니다.
    BackHandler(enabled = isInSelectionMode) {
        selectedApiIds = emptySet()
    }

    /** API 아이템의 선택 상태를 토글하는 함수 */
    fun toggleSelection(apiId: String) {
        selectedApiIds = if (apiId in selectedApiIds) {
            selectedApiIds - apiId
        } else {
            selectedApiIds + apiId
        }
    }

    Scaffold(
        topBar = {
            // 다중 선택 모드일 때와 아닐 때 다른 TopAppBar를 표시합니다.
            if (isInSelectionMode) {
                // 다중 선택 모드 TopAppBar
                TopAppBar(
                    title = { Text("${selectedApiIds.size}개 선택됨") },
                    navigationIcon = {
                        // 선택 해제 버튼
                        IconButton(onClick = { selectedApiIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "선택 해제")
                        }
                    },
                    actions = {
                        // 전체 선택 버튼
                        IconButton(onClick = { selectedApiIds = apiItems.map { it.id }.toSet() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "전체 선택")
                        }
                        // 선택 항목 실행 버튼
                        IconButton(onClick = {
                            confirmDialogTitle = "API 실행"
                            confirmDialogText = "선택한 ${selectedApiIds.size}개의 API를 실행하시겠습니까?"
                            confirmAction = {
                                val selectedItems = apiItems.filter { it.id in selectedApiIds }
                                onExecuteApis(selectedItems)
                                selectedApiIds = emptySet()
                            }
                            showConfirmDialog = true
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "선택 항목 실행")
                        }
                        // 선택 항목 복사 버튼 (1개 선택 시에만 활성화)
                        IconButton(
                            onClick = {
                                confirmDialogTitle = "API 복사"
                                confirmDialogText = "이 API를 복사하시겠습니까?"
                                confirmAction = {
                                    val selectedItem = apiItems.find { it.id == selectedApiIds.first() }
                                    if (selectedItem != null) {
                                        onCopyApi(selectedItem)
                                    }
                                    selectedApiIds = emptySet()
                                }
                                showConfirmDialog = true
                            },
                            enabled = selectedApiIds.size == 1
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "선택 항목 복사")
                        }
                        // 선택 항목 삭제 버튼
                        IconButton(onClick = {
                            confirmDialogTitle = "API 삭제"
                            confirmDialogText = "선택한 ${selectedApiIds.size}개의 API를 삭제하시겠습니까?"
                            confirmAction = {
                                val selectedItems = apiItems.filter { it.id in selectedApiIds }
                                onDeleteApis(selectedItems)
                                selectedApiIds = emptySet()
                            }
                            showConfirmDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "선택 항목 삭제")
                        }
                    }
                )
            } else {
                // 일반 모드 TopAppBar
                TopAppBar(
                    title = { Text("CapyCaller") },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "설정")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            // 다중 선택 모드가 아닐 때만 API 추가 버튼을 표시합니다.
            if (!isInSelectionMode) {
                FloatingActionButton(onClick = onAddApi) {
                    Icon(Icons.Filled.Add, contentDescription = "API 추가")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(apiItems, key = { it.id }) { apiItem ->
                ApiListItem(
                    apiItem = apiItem,
                    isSelected = apiItem.id in selectedApiIds,
                    isInSelectionMode = isInSelectionMode,
                    onToggleSelection = { toggleSelection(apiItem.id) },
                    onNavigateToDetails = { onApiClick(apiItem) }
                )
            }
        }
    }
}

/**
 * API 목록의 각 항목을 표시하는 Composable 입니다.
 */
@Composable
fun ApiListItem(
    apiItem: ApiItem,           // 표시할 API 정보
    isSelected: Boolean,        // 이 항목이 현재 선택되었는지 여부
    isInSelectionMode: Boolean, // 현재 다중 선택 모드인지 여부
    onToggleSelection: () -> Unit, // 선택 상태를 변경하는 콜백
    onNavigateToDetails: () -> Unit // 상세 화면으로 이동하는 콜백
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // 제스처 입력을 처리합니다.
            .pointerInput(isInSelectionMode) {
                detectTapGestures(
                    onLongPress = { onToggleSelection() }, // 길게 누르면 항상 선택 모드로 전환
                    onTap = { // 탭(클릭) 동작
                        if (isInSelectionMode) {
                            onToggleSelection() // 선택 모드에서는 선택/해제
                        } else {
                            onNavigateToDetails() // 일반 모드에서는 상세 화면으로 이동
                        }
                    }
                )
            },
        // 선택된 항목은 다른 배경색으로 표시합니다.
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HttpMethodLabel(method = apiItem.method) // HTTP 메소드 라벨
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = apiItem.name, style = MaterialTheme.typography.titleMedium) // API 이름
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = apiItem.url, style = MaterialTheme.typography.bodySmall, maxLines = 1) // URL (한 줄로 표시)
                if (apiItem.memo?.isNotBlank() == true) { // 메모가 있으면 표시
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = apiItem.memo, style = MaterialTheme.typography.bodySmall, maxLines = 2) // 메모 (최대 두 줄)
                }
            }
        }
    }
}
