package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.theme.SecondaryAccent

@Composable
fun BeatWaveIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF261A3D),
                        Color(0xFF141420)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Organic 5-bar sound pulse wave
        Row(
            modifier = Modifier.padding(horizontal = size * 0.16f),
            horizontalArrangement = Arrangement.spacedBy(size * 0.06f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barHeights = listOf(0.42f, 0.72f, 1.0f, 0.65f, 0.35f)
            val barColors = listOf(
                PrimaryVioletLight,
                PrimaryViolet,
                Color.White,
                PrimaryViolet,
                PrimaryVioletLight
            )

            for (i in barHeights.indices) {
                Box(
                    modifier = Modifier
                        .width(size * 0.09f)
                        .height(size * 0.55f * barHeights[i])
                        .clip(RoundedCornerShape(size * 0.05f))
                        .background(barColors[i])
                )
            }
        }
    }
}

@Composable
fun BeatWaveLogoHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    showSubtitle: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BeatWaveIcon(size = iconSize)
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Beat",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Wave",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = PrimaryViolet,
                    letterSpacing = (-0.5).sp
                )
            )
        }
    }
}
