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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * ApiItem을 cURL 커맨드 문자열로 변환합니다.
 * @param apiItem 변환할 ApiItem 객체.
 * @return 생성된 cURL 커맨드 문자열.
 */
fun toCurlCommand(apiItem: ApiItem): String {
    val curl = StringBuilder("curl")

    // HTTP 메소드 추가
    curl.append(" -X ${apiItem.method}")

    // URL 및 쿼리 파라미터 조합
    val urlBuilder = StringBuilder(apiItem.url)
    if (apiItem.queryParams.isNotEmpty()) {
        urlBuilder.append("?")
        apiItem.queryParams.filter { it.first.isNotBlank() }.forEachIndexed { index, pair ->
            if (index > 0) urlBuilder.append("&")
            urlBuilder.append("${pair.first}=${pair.second}")
        }
    }
    curl.append(" '${urlBuilder}'")

    // 헤더 추가
    apiItem.headers.filter { it.first.isNotBlank() }.forEach { header ->
        curl.append(" -H '${header.first}: ${header.second}'")
    }

    // 본문 추가 (작은따옴표 이스케이프 처리)
    if (apiItem.body.isNotBlank()) {
        curl.append(" -d '${apiItem.body.replace("'", "'\\\\'\'")}'")
    }

    return curl.toString()
}

/**
 * XML 문자열을 보기 좋게 정렬(pretty print)합니다.
 * @param xml 정렬할 XML 문자열.
 * @return 정렬된 XML 문자열. 오류 발생 시 원본 문자열 반환.
 */
fun prettyPrintXml(xml: String): String {
    return try {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val writer = StringWriter()
        transformer.transform(DOMSource(document.documentElement), StreamResult(writer))
        writer.toString()
    } catch (e: Exception) {
        xml // 오류 발생 시 원본 반환
    }
}

/**
 * 코드 문자열(JSON, XML)에 구문 강조(Syntax Highlighting)를 적용한 AnnotatedString을 생성합니다.
 * @param code 구문 강조를 적용할 코드 문자열.
 * @param type 코드의 타입 (e.g., "json", "xml").
 * @return 구문 강조 스타일이 적용된 AnnotatedString.
 */
fun getAnnotatedSyntaxHighlightedString(code: String, type: String): AnnotatedString {
    return buildAnnotatedString {
        when {
            // JSON 타입 구문 강조
            type.contains("json", ignoreCase = true) -> {
                val regex = "\"([^\"]*)\":|(\"[^\"]*\")|([\\d.]+)|(\\[|\\]|\\{|\\})|(true|false)|(null)".toRegex()
                val keyColor = Color(0xFF9876AA)       // 키 색상
                val stringColor = Color(0xFF6A8759)    // 문자열 색상
                val numberColor = Color(0xFF6897BB)    // 숫자 색상
                val keywordColor = Color(0xFFCC7832)   // 키워드(true, false, null) 색상

                var lastIndex = 0
                regex.findAll(code).forEach { matchResult ->
                    val startIndex = matchResult.range.first
                    val endIndex = matchResult.range.last + 1

                    if (startIndex > lastIndex) {
                        append(code.substring(lastIndex, startIndex))
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
                if (lastIndex < code.length) {
                    append(code.substring(lastIndex))
                }
            }
            // XML 타입 구문 강조
            type.contains("xml", ignoreCase = true) -> {
                val xmlRegex = "(<!--.*?-->)|(<\\/?[\\w:.-]+)|(\\s[\\w:.-]+=)|(\"[^\"]*\")|([\\/>?])".toRegex()
                val tagColor = Color(0xFFE8926B)       // 태그 색상
                val attributeColor = Color(0xFF9876AA) // 속성 색상
                val stringColor = Color(0xFF6A8759)    // 문자열 값 색상
                val commentColor = Color(0xFF808080)   // 주석 색상

                var lastIndex = 0
                xmlRegex.findAll(code).forEach { matchResult ->
                    val startIndex = matchResult.range.first
                    val endIndex = matchResult.range.last + 1

                    if (startIndex > lastIndex) {
                        append(code.substring(lastIndex, startIndex))
                    }

                    val (comment, tag, attribute, attrValue, bracket) = matchResult.destructured

                    val (text, style) = when {
                        comment.isNotEmpty() -> matchResult.value to SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)
                        tag.isNotEmpty() -> matchResult.value to SpanStyle(color = tagColor, fontWeight = FontWeight.Bold)
                        attribute.isNotEmpty() -> matchResult.value to SpanStyle(color = attributeColor)
                        attrValue.isNotEmpty() -> matchResult.value to SpanStyle(color = stringColor)
                        bracket.isNotEmpty() -> matchResult.value to SpanStyle(color = Color.Gray)
                        else -> matchResult.value to SpanStyle()
                    }
                    withStyle(style) {
                        append(text)
                    }
                    lastIndex = endIndex
                }
                if (lastIndex < code.length) {
                    append(code.substring(lastIndex))
                }
            }
            // 지원하지 않는 타입은 원본 텍스트 그대로
            else -> {
                append(code)
            }
        }
    }
}

/**
 * TextField에 구문 강조를 적용하기 위한 VisualTransformation 구현체.
 */
private class SyntaxHighlightingVisualTransformation(private val syntaxType: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            getAnnotatedSyntaxHighlightedString(text.text, syntaxType),
            OffsetMapping.Identity // 텍스트 변환이 없으므로 Identity 매핑 사용
        )
    }
}

/**
 * 새로운 API를 추가하거나 기존 API를 수정하는 화면의 Composable 함수.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiEditScreen(
    apiItem: ApiItem?,            // 수정할 API 정보 (추가 시에는 null)
    apiResult: ApiResult?,        // API 실행 결과
    onSave: (ApiItem) -> Unit,    // 저장 버튼 클릭 시 호출될 콜백
    onExecute: (ApiItem) -> Unit, // 실행 버튼 클릭 시 호출될 콜백
    onNavigateBack: () -> Unit,   // 뒤로가기 버튼 클릭 시 호출될 콜백
    apiSettingsViewModel: ApiSettingsViewModel = viewModel()
) {
    // 각 입력 필드의 상태를 관리하는 remember 변수들
    var name by remember(apiItem) { mutableStateOf(apiItem?.name ?: "") }
    var memo by remember(apiItem) { mutableStateOf(apiItem?.memo ?: "") }
    var url by remember(apiItem) { mutableStateOf(apiItem?.url ?: "") }
    var method by remember(apiItem) { mutableStateOf(apiItem?.method ?: "GET") }
    var isShortcut by remember(apiItem) { mutableStateOf(apiItem?.isShortcut ?: false) }
    val baseUrl by apiSettingsViewModel.baseUrl.collectAsState() // 전역 설정의 Base URL

    val queryParams = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.queryParams ?: emptyList()) } }
    val headers = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.headers ?: emptyList()) } }

    var bodyType by remember(apiItem) { mutableStateOf(apiItem?.bodyType ?: "application/json") }
    var body by remember(apiItem) { mutableStateOf(apiItem?.body ?: "") }
    var selectedTab by remember { mutableStateOf(0) } // 'Request' / 'Response' 탭 선택 상태
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    /** 현재 입력된 정보로 ApiItem 객체를 생성하는 함수 */
    fun buildApiItem(): ApiItem {
        // Base URL과 현재 URL을 조합
        val finalUrl = if (baseUrl.isNotBlank() && !url.startsWith("http")) {
            baseUrl + url
        } else {
            url
        }

        return apiItem?.copy(
            name = name,
            memo = memo,
            url = finalUrl,
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
            url = finalUrl,
            method = method,
            headers = headers.filter { it.first.isNotBlank() }.toList(),
            queryParams = queryParams.filter { it.first.isNotBlank() }.toList(),
            bodyType = bodyType,
            body = body,
            isShortcut = isShortcut
        )
    }

    // 화면이 사라질 때 (onDispose) 자동으로 저장하는 효과
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
                title = { Text(if (apiItem == null) "API 추가" else "API 수정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    // cURL로 복사 버튼
                    IconButton(onClick = {
                        val curlCommand = toCurlCommand(buildApiItem())
                        clipboardManager.setText(AnnotatedString(curlCommand))
                        Toast.makeText(context, "cURL로 복사되었습니다", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "cURL로 복사")
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
            // 스크롤 가능한 영역
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("API 이름") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        // 실행 버튼
                        IconButton(onClick = {
                            onExecute(buildApiItem())
                            selectedTab = 1 // 실행 후 응답 탭으로 자동 전환
                        }) {
                            Icon(Icons.Filled.Send, contentDescription = "실행")
                        }
                    },
                    placeholder = { Text(baseUrl) } // Base URL을 힌트로 표시
                )
                Spacer(Modifier.height(8.dp))

                // HTTP 메소드 선택 드롭다운
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
                        label = { Text("메소드") },
                        leadingIcon = {
                            Box(modifier = Modifier.padding(start = 8.dp)) {
                                HttpMethodLabel(method = method) // 선택된 메소드를 색상 라벨로 표시
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

                // 바로가기 추가 체크박스
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isShortcut, onCheckedChange = { isShortcut = it })
                    Text("바로가기에 추가")
                }
                Spacer(Modifier.height(8.dp))

                // 요청/응답 탭
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("요청") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("응답") })
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

/**
 * 요청 관련 탭(파라미터, 헤더, 본문)을 포함하는 Composable.
 */
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
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("파라미터") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("헤더") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("본문") })
        }
        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> KeyValueInput(queryParams, keyLabel = "키")
            1 -> KeyValueInput(headers, keyLabel = "키")
            2 -> BodyInput(body, bodyType, onBodyChange = onBodyChange, onBodyTypeChange = onBodyTypeChange)
        }
    }
}

/**
 * API 실행 응답을 표시하는 탭의 Composable.
 */
@Composable
fun ResponseTab(result: ApiResult?, apiName: String) {
    if (result == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("API를 실행하면 응답이 여기에 표시됩니다.")
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
                    Text("상태: ${result.code}", fontWeight = FontWeight.Bold)
                    Text("시간: ${result.time}ms", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("본문") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("헤더") })
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
                                Toast.makeText(context, "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(context, "다운로드 폴더에 저장되었습니다", Toast.LENGTH_SHORT).show()
                                            }
                                        } ?: throw IOException("MediaStore 레코드를 생성하지 못했습니다.")
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "다운로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
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
                 Text("오류: ${result.message}", color = Color.Red)
            }
        }
    }
}

/**
 * 형식화된(pretty-printed) 응답 본문을 표시하고 복사, 공유, 다운로드 기능을 제공하는 Composable.
 */
@Composable
fun FormattedBody(
    data: String, 
    contentType: String,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownload: (String) -> Unit
) {
    // Content-Type에 따라 데이터를 보기 좋게 형식화
    val prettyData = when {
        contentType.contains("json", ignoreCase = true) -> try {
            GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(data))
        } catch (e: JsonSyntaxException) { data } // JSON 파싱 실패 시 원본 데이터 반환
        contentType.contains("xml", ignoreCase = true) -> prettyPrintXml(data)
        else -> data
    }

    // 형식화된 데이터에 구문 강조 적용
    val annotatedString = getAnnotatedSyntaxHighlightedString(prettyData, contentType)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { onCopy(prettyData) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "복사")
            }
            IconButton(onClick = { onShare(prettyData) }) {
                Icon(Icons.Filled.Share, contentDescription = "공유")
            }
            IconButton(onClick = { onDownload(prettyData) }) {
                Icon(Icons.Filled.Download, contentDescription = "다운로드")
            }
        }
        // 텍스트를 선택하고 복사할 수 있도록 SelectionContainer 사용
        SelectionContainer {
            Text(
                text = annotatedString,
                fontFamily = FontFamily.Monospace, // 고정폭 글꼴 사용
                modifier = Modifier.horizontalScroll(rememberScrollState()) // 가로 스크롤 가능
            )
        }
    }
}

/**
 * 요청 본문과 본문 타입을 입력받는 Composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyInput(body: String, bodyType: String, onBodyChange: (String) -> Unit, onBodyTypeChange: (String) -> Unit) {
     var bodyTypeMenuExpanded by remember { mutableStateOf(false) }
     // Body Type 선택 드롭다운
     ExposedDropdownMenuBox(
         expanded = bodyTypeMenuExpanded,
         onExpandedChange = { bodyTypeMenuExpanded = !bodyTypeMenuExpanded },
     ) {
        OutlinedTextField(
            value = bodyType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Body 타입") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bodyTypeMenuExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = bodyTypeMenuExpanded,
            onDismissRequest = { bodyTypeMenuExpanded = false },
            modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
        ) {
            listOf("application/json", "text/plain", "application/x-www-form-urlencoded", "application/xml").forEach { selection ->
                DropdownMenuItem(text = { Text(selection) }, onClick = {
                    onBodyTypeChange(selection)
                    bodyTypeMenuExpanded = false
                })
            }
        }
    }
    // 요청 본문 입력 필드
    OutlinedTextField(
        value = body,
        onValueChange = onBodyChange,
        label = { Text("요청 본문") },
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 150.dp),
        // 선택된 Body Type에 따라 구문 강조 적용
        visualTransformation = SyntaxHighlightingVisualTransformation(bodyType)
    )
}

/**
 * HTTP 메소드 이름을 색상있는 라벨로 표시하는 Composable.
 */
@Composable
fun HttpMethodLabel(method: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(getHttpMethodColor(method)) // 메소드에 따라 다른 배경색 적용
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
