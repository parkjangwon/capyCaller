package org.parkjw.capycaller.ui.theme

import androidx.compose.ui.graphics.Color

fun getHttpMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF61AFFE) // Blue
        "POST" -> Color(0xFF49CC90) // Green
        "PUT" -> Color(0xFFFCA130) // Orange
        "DELETE" -> Color(0xFFF93E3E) // Red
        "PATCH" -> Color(0xFFD5B400) // Yellow
        else -> Color.Gray
    }
}
