package org.parkjw.capycaller.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiItem(
    val id: String,
    val name: String,
    val url: String,
    val method: String,
    val headers: List<Pair<String, String>> = emptyList(),
    val queryParams: List<Pair<String, String>> = emptyList(),
    val body: String = "",
    val bodyType: String = "application/json",
    var isShortcut: Boolean = false,
    val memo: String = ""
) : Parcelable
