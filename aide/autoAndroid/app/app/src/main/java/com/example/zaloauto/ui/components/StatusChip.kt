package com.example.zaloauto.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        "PENDING" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        "SENT" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "FAILED" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        "CANCELED" -> Pair(Color(0xFFF5F5F5), Color(0xFF757575))
        "RETRYING" -> Pair(Color(0xFFFFF8E1), Color(0xFFF9A825))
        else -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
