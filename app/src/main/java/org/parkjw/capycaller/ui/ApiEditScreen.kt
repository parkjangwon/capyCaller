package org.parkjw.capycaller.ui

import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiResult
import org.parkjw.capycaller.ui.theme.getHttpMethodColor
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun toCurlCommand(apiItem: ApiItem): String {
    val curl = StringBuilder("curl")

    // Method
    curl.append(" -X ${apiItem.method}")

    // URL and Query Params
    val urlBuilder = StringBuilder(apiItem.url)
    if (apiItem.queryParams.isNotEmpty()) {
        urlBuilder.append("?")
        apiItem.queryParams.filter { it.first.isNotBlank() }.forEachIndexed { index, pair ->
            if (index > 0) urlBuilder.append("&")
            urlBuilder.append("${pair.first}=${pair.second}")
        }
    }
    curl.append(" '${urlBuilder}'")

    // Headers
    apiItem.headers.filter { it.first.isNotBlank() }.forEach { header ->
        curl.append(" -H '${header.first}: ${header.second}'")
    }

    // Body
    if (apiItem.body.isNotBlank()) {
        curl.append(" -d '${apiItem.body.replace("'", "'\\''")}'")
    }

    return curl.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiEditScreen(
    apiItem: ApiItem?,
    apiResult: ApiResult?,
    onSave: (ApiItem) -> Unit,
    onExecute: (ApiItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember(apiItem) { mutableStateOf(apiItem?.name ?: "") }
    var memo by remember(apiItem) { mutableStateOf(apiItem?.memo ?: "") }
    var url by remember(apiItem) { mutableStateOf(apiItem?.url ?: "") }
    var method by remember(apiItem) { mutableStateOf(apiItem?.method ?: "GET") }
    var isShortcut by remember(apiItem) { mutableStateOf(apiItem?.isShortcut ?: false) }

    val queryParams = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.queryParams ?: emptyList()) } }
    val headers = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.headers ?: emptyList()) } }

    var bodyType by remember(apiItem) { mutableStateOf(apiItem?.bodyType ?: "application/json") }
    var body by remember(apiItem) { mutableStateOf(apiItem?.body ?: "") }
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    fun buildApiItem(): ApiItem {
        return apiItem?.copy(
            name = name,
            memo = memo,
            url = url,
            method = method,
            headers = headers.filter { it.first.isNotBlank() }.toList(),
            queryParams = queryParams.filter { it.first.isNotBlank() }.toList(),
            bodyType = bodyType,
            body = body,
            isShortcut = isShortcut
        ) ?: ApiItem(
            id = apiItem?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            memo = memo,
            url = url,
            method = method,
            headers = headers.filter { it.first.isNotBlank() }.toList(),
            queryParams = queryParams.filter { it.first.isNotBlank() }.toList(),
            bodyType = bodyType,
            body = body,
            isShortcut = isShortcut
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
                },
                actions = {
                    IconButton(onClick = {
                        val curlCommand = toCurlCommand(buildApiItem())
                        clipboardManager.setText(AnnotatedString(curlCommand))
                        Toast.makeText(context, "Copied as cURL", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy as cURL")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("API Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Memo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            onExecute(buildApiItem())
                            selectedTab = 1
                        }) {
                            Icon(Icons.Filled.Send, contentDescription = "Execute")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

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
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isShortcut, onCheckedChange = { isShortcut = it })
                    Text("Add to shortcuts")
                }
                Spacer(Modifier.height(8.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Request") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Response") })
                }
                Spacer(Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> RequestTabs(
                        queryParams = queryParams,
                        headers = headers,
                        body = body,
                        bodyType = bodyType,
                        onBodyChange = { body = it },
                        onBodyTypeChange = { bodyType = it }
                    )
                    1 -> ResponseTab(apiResult, name)
                }
            }
        }
    }
}

@Composable
fun RequestTabs(
    queryParams: SnapshotStateList<Pair<String, String>>,
    headers: SnapshotStateList<Pair<String, String>>,
    body: String,
    bodyType: String,
    onBodyChange: (String) -> Unit,
    onBodyTypeChange: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Params") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Headers") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Body") })
        }
        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> KeyValueInput(queryParams, keyLabel = "Parameter")
            1 -> KeyValueInput(headers, keyLabel = "Header")
            2 -> BodyInput(body, bodyType, onBodyChange = onBodyChange, onBodyTypeChange = onBodyTypeChange)
        }
    }
}

@Composable
fun ResponseTab(result: ApiResult?, apiName: String) {
    if (result == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Execute an API call to see the response.")
        }
        return
    }

    when (result) {
        is ApiResult.Success -> {
            var selectedTab by remember { mutableStateOf(0) }
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            val coroutineScope = rememberCoroutineScope()

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Status: ${result.code}", fontWeight = FontWeight.Bold)
                    Text("Time: ${result.time}ms", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Body") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Headers") })
                }
                Spacer(Modifier.height(8.dp))
                when (selectedTab) {
                    0 -> {
                        val contentType = result.headers.entries.find { it.key.equals("content-type", true) }?.value ?: ""
                        FormattedBody(
                            data = result.data,
                            contentType = contentType,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, it)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onDownload = { content ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val resolver = context.contentResolver

                                        val (extension, mimeType) = when {
                                            contentType.contains("json", true) -> "json" to "application/json"
                                            contentType.contains("xml", true) -> "xml" to "application/xml"
                                            else -> "txt" to "text/plain"
                                        }

                                        val timeStamp = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
                                        val finalApiName = if (apiName.isNotBlank()) apiName else "untitled"
                                        val fileName = "capyCaller-${finalApiName}-response-$timeStamp.$extension"

                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                        }
                                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                        uri?.let {
                                            resolver.openOutputStream(it)?.use { outputStream ->
                                                outputStream.write(content.toByteArray())
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Downloaded to Downloads folder", Toast.LENGTH_SHORT).show()
                                            }
                                        } ?: throw IOException("Failed to create new MediaStore record.")
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    1 -> result.headers.forEach { (key, value) ->
                        Row {
                            Text("$key: ", fontWeight = FontWeight.Bold)
                            Text(value)
                        }
                    }
                }
            }
        }
        is ApiResult.Error -> {
            Column {
                 Text("Error: ${result.message}", color = Color.Red)
            }
        }
    }
}

@Composable
fun FormattedBody(
    data: String, 
    contentType: String,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownload: (String) -> Unit
) {
    val prettyJson = if (contentType.contains("json", ignoreCase = true)) {
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(JsonParser.parseString(data))
        } catch (e: JsonSyntaxException) {
            data // Not a valid json, return original data
        } catch (e: Exception) {
            data
        }
    } else {
        data
    }

    val annotatedString = buildAnnotatedString {
        if (contentType.contains("json", ignoreCase = true) && prettyJson != data) {
            val regex = "\"([^\"]*)\":|(\"[^\"]*\")|([\\d.]+)|(\\[|\\]|\\{|\\})|(true|false)|(null)".toRegex()

            val keyColor = Color(0xFF9876AA)
            val stringColor = Color(0xFF6A8759)
            val numberColor = Color(0xFF6897BB)
            val keywordColor = Color(0xFFCC7832)

            var lastIndex = 0
            regex.findAll(prettyJson).forEach { matchResult ->
                val startIndex = matchResult.range.first
                val endIndex = matchResult.range.last + 1

                if (startIndex > lastIndex) {
                    append(prettyJson.substring(lastIndex, startIndex))
                }

                val (key, string, number, bracket, boolean, nullVal) = matchResult.destructured

                val (text, style) = when {
                    key.isNotEmpty() -> matchResult.value to SpanStyle(color = keyColor, fontWeight = FontWeight.Bold)
                    string.isNotEmpty() -> matchResult.value to SpanStyle(color = stringColor)
                    number.isNotEmpty() -> matchResult.value to SpanStyle(color = numberColor)
                    boolean.isNotEmpty() -> matchResult.value to SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)
                    nullVal.isNotEmpty() -> matchResult.value to SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)
                    bracket.isNotEmpty() -> matchResult.value to SpanStyle(color = Color.Gray)
                    else -> matchResult.value to SpanStyle()
                }
                withStyle(style) {
                    append(text)
                }
                lastIndex = endIndex
            }
            if (lastIndex < prettyJson.length) {
                append(prettyJson.substring(lastIndex))
            }
        } else {
            append(prettyJson)
        }
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { onCopy(prettyJson) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = { onShare(prettyJson) }) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(onClick = { onDownload(prettyJson) }) {
                Icon(Icons.Filled.Download, contentDescription = "Download")
            }
        }
        SelectionContainer {
            Text(
                text = annotatedString,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
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
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 150.dp)
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
