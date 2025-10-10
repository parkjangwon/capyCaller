package org.parkjw.capycaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit, 
    settingsViewModel: SettingsViewModel = viewModel(),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeSettings(settingsViewModel)
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
