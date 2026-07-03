package com.mystarrail.tool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystarrail.tool.data.model.Tier

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    val (bg, label) = when (tier) {
        Tier.S -> Color(0xFFFFD700) to "S"
        Tier.A -> Color(0xFFB388FF) to "A"
        Tier.B -> Color(0xFF64B5F6) to "B"
        Tier.C -> Color(0xFF9E9E9E) to "C"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}