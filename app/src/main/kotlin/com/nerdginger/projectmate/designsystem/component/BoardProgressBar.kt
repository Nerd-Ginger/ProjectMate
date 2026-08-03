package com.nerdginger.projectmate.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nerdginger.projectmate.core.model.BoardProgress
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.designsystem.ProjectMateTheme

/** Colour for a status category, used anywhere progress is shown. */
fun StatusCategory.indicatorColor(): Color = when (this) {
    StatusCategory.INBOX -> Color(0xFF94A3B8)
    StatusCategory.BACKLOG -> Color(0xFF64748B)
    StatusCategory.ACTIVE -> Color(0xFF6366F1)
    StatusCategory.BLOCKED -> Color(0xFFEF4444)
    StatusCategory.DONE -> Color(0xFF22C55E)
    StatusCategory.CANCELLED -> Color(0xFF78716C)
}

/**
 * A board's composition, by status category.
 *
 * Deliberately segmented rather than a percentage fill. A single number can't
 * show that a board is one-third blocked, and "how much of this is stuck" is
 * the question a projects board most needs to answer at a glance.
 *
 * Segments follow workflow order — inbox through done — so the bar reads
 * left-to-right the way the board does.
 */
@Composable
fun BoardProgressBar(
    progress: BoardProgress,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val segments = SEGMENT_ORDER.mapNotNull { category ->
        val count = progress.countsByCategory[category] ?: 0
        if (count > 0) category to count else null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        segments.forEach { (category, count) ->
            androidx.compose.foundation.layout.Box(
                Modifier
                    .weight(count.toFloat())
                    .fillMaxWidth()
                    .height(height)
                    .background(category.indicatorColor()),
            )
        }
    }
}

private val SEGMENT_ORDER = listOf(
    StatusCategory.INBOX,
    StatusCategory.BACKLOG,
    StatusCategory.ACTIVE,
    StatusCategory.BLOCKED,
    StatusCategory.DONE,
    StatusCategory.CANCELLED,
)

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun BoardProgressBarPreview() {
    ProjectMateTheme(dynamicColor = false) {
        BoardProgressBar(
            progress = BoardProgress(
                mapOf(
                    StatusCategory.BACKLOG to 4,
                    StatusCategory.ACTIVE to 2,
                    StatusCategory.BLOCKED to 1,
                    StatusCategory.DONE to 5,
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
