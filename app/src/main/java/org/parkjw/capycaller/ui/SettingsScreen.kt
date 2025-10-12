package org.parkjw.capycaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 앱의 다양한 설정을 변경할 수 있는 화면의 Composable 함수입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit, // 뒤로가기 콜백
    settingsViewModel: SettingsViewModel = viewModel(), // 일반 설정 ViewModel
    apiSettingsViewModel: ApiSettingsViewModel = viewModel(), // API 관련 설정 ViewModel
    onBackupClick: () -> Unit, // 백업 버튼 클릭 콜백
    onRestoreClick: () -> Unit // 복원 버튼 클릭 콜백
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        // 스크롤 가능한 Column
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp) // 각 설정 섹션 사이의 간격
        ) {
            ThemeSettings(settingsViewModel)
            NotificationSettings(settingsViewModel)
            ApiSettings(apiSettingsViewModel)
            BackupRestoreSettings(onBackupClick, onRestoreClick)
        }
    }
}

/**
 * 테마 설정(시스템, 라이트, 다크)을 위한 Composable 입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettings(viewModel: SettingsViewModel) {
    var themeDropDownExpanded by remember { mutableStateOf(false) }
    val themeOptions = listOf("System", "Light", "Dark")
    val selectedTheme by viewModel.theme.collectAsState() // ViewModel로부터 현재 테마 상태를 구독

    Column {
        Text("테마", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = themeDropDownExpanded,
            onExpandedChange = { themeDropDownExpanded = !themeDropDownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTheme,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeDropDownExpanded) }
            )
            ExposedDropdownMenu(
                expanded = themeDropDownExpanded,
                onDismissRequest = { themeDropDownExpanded = false }
            ) {
                themeOptions.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme) },
                        onClick = {
                            viewModel.setTheme(theme) // 선택된 테마를 ViewModel에 저장
                            themeDropDownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 알림 설정(푸시 알림 사용 여부)을 위한 Composable 입니다.
 */
@Composable
fun NotificationSettings(viewModel: SettingsViewModel) {
    val usePushNotifications by viewModel.usePushNotifications.collectAsState()

    Column {
        Text("알림", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("푸시 알림 사용")
            Switch(
                checked = usePushNotifications,
                onCheckedChange = { viewModel.setUsePushNotifications(it) } // 스위치 상태 변경 시 ViewModel에 저장
            )
        }
    }
}

/**
 * API 관련 전역 설정(SSL, 타임아웃 등)을 위한 Composable 입니다.
 */
@Composable
fun ApiSettings(viewModel: ApiSettingsViewModel) {
    // ViewModel로부터 각 설정 값의 상태를 구독합니다.
    val ignoreSslErrors by viewModel.ignoreSslErrors.collectAsState()
    val useCookieJar by viewModel.useCookieJar.collectAsState()
    val sendNoCache by viewModel.sendNoCache.collectAsState()
    val followRedirects by viewModel.followRedirects.collectAsState()

    // TextField의 즉각적인 업데이트를 위해 로컬 상태 변수를 사용하고,
    // 포커스가 해제될 때 ViewModel에 최종 값을 저장하는 방식을 사용합니다.
    val baseUrlFromVm by viewModel.baseUrl.collectAsState()
    var localBaseUrl by remember(baseUrlFromVm) { mutableStateOf(baseUrlFromVm) }

    val connectTimeoutFromVm by viewModel.connectTimeout.collectAsState()
    var localConnectTimeout by remember(connectTimeoutFromVm) { mutableStateOf((connectTimeoutFromVm / 1000).toString()) }

    val readTimeoutFromVm by viewModel.readTimeout.collectAsState()
    var localReadTimeout by remember(readTimeoutFromVm) { mutableStateOf((readTimeoutFromVm / 1000).toString()) }

    val writeTimeoutFromVm by viewModel.writeTimeout.collectAsState()
    var localWriteTimeout by remember(writeTimeoutFromVm) { mutableStateOf((writeTimeoutFromVm / 1000).toString()) }

    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("API 설정", style = MaterialTheme.typography.titleMedium)

        // SSL 오류 무시 스위치
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SSL 인증서 오류 무시")
            Switch(
                checked = ignoreSslErrors,
                onCheckedChange = { viewModel.setIgnoreSslErrors(it) }
            )
        }

        // 쿠키 저장소 사용 스위치
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("쿠키 저장소 사용")
            Switch(
                checked = useCookieJar,
                onCheckedChange = { viewModel.setUseCookieJar(it) }
            )
        }

        // no-cache 헤더 전송 스위치
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("no-cache 헤더 전송")
            Switch(
                checked = sendNoCache,
                onCheckedChange = { viewModel.setSendNoCache(it) }
            )
        }

        // 리다이렉트 자동 처리 스위치
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("리다이렉트 자동 처리")
            Switch(
                checked = followRedirects,
                onCheckedChange = { viewModel.setFollowRedirects(it) }
            )
        }

        // 기본 URL 입력 필드
        OutlinedTextField(
            value = localBaseUrl,
            onValueChange = { localBaseUrl = it },
            label = { Text("기본 URL") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    // 포커스를 잃었을 때, 그리고 값이 변경되었을 때만 ViewModel 업데이트
                    if (!it.isFocused && localBaseUrl != baseUrlFromVm) {
                        viewModel.setBaseUrl(localBaseUrl)
                    }
                },
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }) // 완료 버튼 시 키보드 숨김
        )

        // 연결 타임아웃 입력 필드
        OutlinedTextField(
            value = localConnectTimeout,
            onValueChange = { localConnectTimeout = it },
            label = { Text("연결 타임아웃 (초)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // 숫자 키보드
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused && localConnectTimeout != (connectTimeoutFromVm / 1000).toString()) {
                        // 초 단위를 밀리초로 변환하여 저장
                        viewModel.setConnectTimeout((localConnectTimeout.toLongOrNull() ?: 60) * 1000)
                    }
                },
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        // 읽기 타임아웃 입력 필드
        OutlinedTextField(
            value = localReadTimeout,
            onValueChange = { localReadTimeout = it },
            label = { Text("읽기 타임아웃 (초)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused && localReadTimeout != (readTimeoutFromVm / 1000).toString()) {
                        viewModel.setReadTimeout((localReadTimeout.toLongOrNull() ?: 60) * 1000)
                    }
                },
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        // 쓰기 타임아웃 입력 필드
        OutlinedTextField(
            value = localWriteTimeout,
            onValueChange = { localWriteTimeout = it },
            label = { Text("쓰기 타임아웃 (초)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused && localWriteTimeout != (writeTimeoutFromVm / 1000).toString()) {
                        viewModel.setWriteTimeout((localWriteTimeout.toLongOrNull() ?: 60) * 1000)
                    }
                },
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

/**
 * 데이터 백업 및 복원 버튼을 위한 Composable 입니다.
 */
@Composable
fun BackupRestoreSettings(onBackupClick: () -> Unit, onRestoreClick: () -> Unit) {
    Column {
        Text("데이터 백업 & 복원", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBackupClick) {
                Text("백업")
            }
            Button(onClick = onRestoreClick) {
                Text("복원")
            }
        }
    }
}
