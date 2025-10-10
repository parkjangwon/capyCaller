package org.parkjw.capycaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.ui.theme.getHttpMethodColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiEditScreen(
    apiItem: ApiItem?,
    onSave: (ApiItem) -> Unit,
    onExecute: (ApiItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember(apiItem) { mutableStateOf(apiItem?.name ?: "") }
    var url by remember(apiItem) { mutableStateOf(apiItem?.url ?: "") }
    var method by remember(apiItem) { mutableStateOf(apiItem?.method ?: "GET") }

    val queryParams = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.queryParams ?: emptyList()) } }
    val headers = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.headers ?: emptyList()) } }

    var bodyType by remember(apiItem) { mutableStateOf(apiItem?.bodyType ?: "application/json") }
    var body by remember(apiItem) { mutableStateOf(apiItem?.body ?: "") }
    var selectedTab by remember { mutableStateOf(0) }

    fun buildApiItem(): ApiItem {
        return apiItem?.copy(
            name = name,
            url = url,
            method = method,
            headers = headers.toList(),
            queryParams = queryParams.toList(),
            bodyType = bodyType,
            body = body
        ) ?: ApiItem(
            id = apiItem?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            url = url,
            method = method,
            headers = headers.toList(),
            queryParams = queryParams.toList(),
            bodyType = bodyType,
            body = body
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            if (name.isNotBlank() || url.isNotBlank()) {
                onSave(buildApiItem())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (apiItem == null) "Add API" else "Edit API") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("API Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { onExecute(buildApiItem()) }) {
                        Icon(Icons.Filled.Send, contentDescription = "Execute")
                    }
                }
            )

            var methodMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = methodMenuExpanded,
                onExpandedChange = { methodMenuExpanded = !methodMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Method") },
                    leadingIcon = {
                        Box(modifier = Modifier.padding(start = 8.dp)) {
                            HttpMethodLabel(method = method)
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = methodMenuExpanded,
                    onDismissRequest = { methodMenuExpanded = false },
                    modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
                ) {
                    listOf("GET", "POST", "PUT", "DELETE", "PATCH").forEach { selection ->
                        DropdownMenuItem(
                            text = { HttpMethodLabel(method = selection) },
                            onClick = {
                                method = selection
                                methodMenuExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Params") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Headers") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Body") })
            }

            when (selectedTab) {
                0 -> KeyValueInput(queryParams, keyLabel = "Parameter")
                1 -> KeyValueInput(headers, keyLabel = "Header")
                2 -> BodyInput(body, bodyType, onBodyChange = { body = it }, onBodyTypeChange = { bodyType = it })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyInput(body: String, bodyType: String, onBodyChange: (String) -> Unit, onBodyTypeChange: (String) -> Unit) {
     var bodyTypeMenuExpanded by remember { mutableStateOf(false) }
     ExposedDropdownMenuBox(
         expanded = bodyTypeMenuExpanded,
         onExpandedChange = { bodyTypeMenuExpanded = !bodyTypeMenuExpanded },
     ) {
        OutlinedTextField(
            value = bodyType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Body Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bodyTypeMenuExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = bodyTypeMenuExpanded,
            onDismissRequest = { bodyTypeMenuExpanded = false },
            modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
        ) {
            listOf("application/json", "text/plain", "application/x-www-form-urlencoded").forEach { selection ->
                DropdownMenuItem(text = { Text(selection) }, onClick = {
                    onBodyTypeChange(selection)
                    bodyTypeMenuExpanded = false
                })
            }
        }
    }
    OutlinedTextField(
        value = body,
        onValueChange = onBodyChange,
        label = { Text("Request Body") },
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp)
    )
}

@Composable
fun HttpMethodLabel(method: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(getHttpMethodColor(method))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = method,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
