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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    apiSettingsViewModel: ApiSettingsViewModel = viewModel(),
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeSettings(settingsViewModel)
            NotificationSettings(settingsViewModel)
            ApiSettings(apiSettingsViewModel)
            BackupRestoreSettings(onBackupClick, onRestoreClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettings(viewModel: SettingsViewModel) {
    var themeDropDownExpanded by remember { mutableStateOf(false) }
    val themeOptions = listOf("System", "Light", "Dark")
    val selectedTheme by viewModel.theme.collectAsState()

    Column {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
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
                            viewModel.setTheme(theme)
                            themeDropDownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationSettings(viewModel: SettingsViewModel) {
    val usePushNotifications by viewModel.usePushNotifications.collectAsState()

    Column {
        Text("Notifications", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Use push notifications")
            Switch(
                checked = usePushNotifications,
                onCheckedChange = { viewModel.setUsePushNotifications(it) }
            )
        }
    }
}

@Composable
fun ApiSettings(viewModel: ApiSettingsViewModel) {
    val ignoreSslErrors by viewModel.ignoreSslErrors.collectAsState()
    val useCookieJar by viewModel.useCookieJar.collectAsState()
    val sendNoCache by viewModel.sendNoCache.collectAsState()
    val followRedirects by viewModel.followRedirects.collectAsState()

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
        Text("API Settings", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ignore SSL errors")
            Switch(
                checked = ignoreSslErrors,
                onCheckedChange = { viewModel.setIgnoreSslErrors(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Use cookie jar")
            Switch(
                checked = useCookieJar,
                onCheckedChange = { viewModel.setUseCookieJar(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Send no-cache header")
            Switch(
                checked = sendNoCache,
                onCheckedChange = { viewModel.setSendNoCache(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Automatically follow redirects")
            Switch(
                checked = followRedirects,
                onCheckedChange = { viewModel.setFollowRedirects(it) }
            )
        }

        OutlinedTextField(
            value = localBaseUrl,
            onValueChange = { localBaseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused && localBaseUrl != baseUrlFromVm) {
                        viewModel.setBaseUrl(localBaseUrl)
                    }
                },
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        OutlinedTextField(
            value = localConnectTimeout,
            onValueChange = { localConnectTimeout = it },
            label = { Text("Connect Timeout (s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused && localConnectTimeout != (connectTimeoutFromVm / 1000).toString()) {
                        viewModel.setConnectTimeout((localConnectTimeout.toLongOrNull() ?: 60) * 1000)
                    }
                },
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        OutlinedTextField(
            value = localReadTimeout,
            onValueChange = { localReadTimeout = it },
            label = { Text("Read Timeout (s)") },
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

        OutlinedTextField(
            value = localWriteTimeout,
            onValueChange = { localWriteTimeout = it },
            label = { Text("Write Timeout (s)") },
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

@Composable
fun BackupRestoreSettings(onBackupClick: () -> Unit, onRestoreClick: () -> Unit) {
    Column {
        Text("Data Backup & Restore", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBackupClick) {
                Text("Backup")
            }
            Button(onClick = onRestoreClick) {
                Text("Restore")
            }
        }
    }
}
