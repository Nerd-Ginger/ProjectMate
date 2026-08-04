package com.nerdginger.projectmate.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.model.BoardProgress
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import com.nerdginger.projectmate.designsystem.ProjectMateTheme

/**
 * Colour for a status category, from the design comp's palette.
 *
 * Active is the accent orange: in-flight work is what the accent exists for.
 * Inbox and cancelled reuse neutrals rather than inventing hues the comp
 * doesn't have.
 */
@Composable
fun StatusCategory.indicatorColor(): Color {
    val t = LocalProjectMateTokens.current
    return when (this) {
        StatusCategory.INBOX -> t.backlog
        StatusCategory.BACKLOG -> t.backlog
        StatusCategory.ACTIVE -> t.active
        StatusCategory.BLOCKED -> t.blocked
        StatusCategory.DONE -> t.done
        StatusCategory.CANCELLED -> t.done
    }
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
    height: Dp = 6.dp,
) {
    val tokens = LocalProjectMateTokens.current
    val segments = SEGMENT_ORDER.mapNotNull { category ->
        val count = progress.countsByCategory[category] ?: 0
        if (count > 0) category to count else null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(tokens.track),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { (category, count) ->
            Box(
                Modifier
                    .weight(count.toFloat())
                    .fillMaxHeight()
                    .background(category.indicatorColor()),
            )
        }
    }
}

/**
 * The count-per-category row beneath the bar — "2 backlog · 2 active · …".
 *
 * Blocked is the one entry that colours its label as well as its swatch. It's
 * the only category the design lets shout, and it's the one worth noticing.
 */
@Composable
fun BoardProgressLegend(
    progress: BoardProgress,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalProjectMateTokens.current
    val entries = LEGEND_ORDER.map { category ->
        category to (progress.countsByCategory[category] ?: 0)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEach { (category, count) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(category.indicatorColor()),
                )
                Text(
                    text = "$count ${category.legendLabel()}",
                    style = tokens.mono.copy(
                        fontSize = 10.sp,
                        color = if (category == StatusCategory.BLOCKED && count > 0) {
                            tokens.blockedText
                        } else {
                            tokens.mono.color
                        },
                    ),
                )
            }
        }
    }
}

private fun StatusCategory.legendLabel(): String = when (this) {
    StatusCategory.INBOX -> "inbox"
    StatusCategory.BACKLOG -> "backlog"
    StatusCategory.ACTIVE -> "active"
    StatusCategory.BLOCKED -> "blocked"
    StatusCategory.DONE -> "done"
    StatusCategory.CANCELLED -> "cancelled"
}

private val SEGMENT_ORDER = listOf(
    StatusCategory.INBOX,
    StatusCategory.BACKLOG,
    StatusCategory.ACTIVE,
    StatusCategory.BLOCKED,
    StatusCategory.DONE,
    StatusCategory.CANCELLED,
)

/** The four the comp shows. Inbox and cancelled aren't worth a slot here. */
private val LEGEND_ORDER = listOf(
    StatusCategory.BACKLOG,
    StatusCategory.ACTIVE,
    StatusCategory.BLOCKED,
    StatusCategory.DONE,
)

@Preview(showBackground = true, widthDp = 320, backgroundColor = 0xFF0B0B0C)
@Composable
private fun BoardProgressBarPreview() {
    ProjectMateTheme {
        val progress = BoardProgress(
            mapOf(
                StatusCategory.BACKLOG to 4,
                StatusCategory.ACTIVE to 2,
                StatusCategory.BLOCKED to 1,
                StatusCategory.DONE to 5,
            ),
        )
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
            BoardProgressBar(progress)
            BoardProgressLegend(progress, Modifier.padding(top = 9.dp))
        }
    }
}
