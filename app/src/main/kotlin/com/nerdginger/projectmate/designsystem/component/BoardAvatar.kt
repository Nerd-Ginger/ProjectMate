package com.nerdginger.projectmate.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A board's two-letter monogram on a tinted square, in the board's accent.
 *
 * The comp uses these rather than the emoji the data model carries — they stay
 * legible at 22dp and give every board the same visual weight. Shared because
 * four screens identify a board this way: the Boards home, Today, Search and
 * Item detail. They differ only in size.
 */
@Composable
fun BoardAvatar(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    // The comp scales the corner and the glyph with the tile rather than
    // keeping them fixed, so a small badge doesn't look like a shrunk big one.
    val corner = size * 9f / 34f
    val glyph = size.value * 12f / 34f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram(name),
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontSize = glyph.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

/**
 * First letters of the first two words, else the first two characters.
 *
 * Uppercased, so "portal v2" and "Portal V2" produce the same badge.
 */
fun monogram(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        else -> name.trim().take(2).uppercase()
    }
}
