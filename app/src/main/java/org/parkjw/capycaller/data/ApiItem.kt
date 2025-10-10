package org.parkjw.capycaller.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val method: String,
    val headers: List<Pair<String, String>>,
    val queryParams: List<Pair<String, String>>,
    val bodyType: String,
    val body: String
) : Parcelable
