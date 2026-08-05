package com.nerdginger.projectmate.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.focus.FocusEntry
import com.nerdginger.projectmate.core.focus.FocusReason
import com.nerdginger.projectmate.core.focus.FocusSection
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.time.DueBucket
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import com.nerdginger.projectmate.designsystem.ProjectMateTheme
import com.nerdginger.projectmate.designsystem.component.BoardAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What needs attention now, across every board.
 *
 * The screen the app is judged on, so it stays narrow on purpose — three
 * sections, nothing configurable, no counts of things you don't have to do.
 * Every decision about what appears is [com.nerdginger.projectmate.core.focus.TodayRules]'
 * in `:core`; this only draws the answer.
 *
 * Stateless, like the other screens: state in, lambdas out.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (state.isEmpty) {
        TodayEmpty(modifier.padding(contentPadding))
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        state.sections.forEach { section ->
            item(key = "header-${section.reason}") {
                SectionHeader(section)
            }
            items(section.entries, key = { it.item.id }) { entry ->
                FocusRow(
                    entry = entry,
                    board = state.boardsById[entry.item.boardId],
                    zone = ZoneId.systemDefault(),
                    onClick = { onOpenItem(entry.item.id) },
                )
            }
        }
    }
}

/**
 * The date, above the title.
 *
 * Rendered by the app shell rather than here, because every top-level screen
 * shares one header. Exposed so the shell can ask Today what its eyebrow says.
 */
fun todayEyebrow(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

@Composable
private fun SectionHeader(section: FocusSection) {
    val tokens = LocalProjectMateTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
    ) {
        Text(
            text = section.reason.title(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            color = section.reason.color(),
        )
        Text(
            text = section.count.toString(),
            style = tokens.mono.copy(fontSize = 11.sp, color = Color(0xFF6E6B67)),
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(tokens.divider),
        )
    }
}

@Composable
private fun FocusRow(
    entry: FocusEntry,
    board: Board?,
    zone: ZoneId,
    onClick: () -> Unit,
) {
    val tokens = LocalProjectMateTokens.current
    val accent = board?.let { Color(it.accentColor) } ?: MaterialTheme.colorScheme.primary
    val statusColor = Color(entry.status.colorArgb)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BoardAvatar(
            name = board?.name ?: "?",
            accent = accent,
            size = 26.dp,
        )

        Column(Modifier.weight(1f)) {
            Text(
                text = entry.item.title,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.padding(top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(entry.status.name, statusColor)

                val due = dueLabel(entry, zone)
                if (due != null) {
                    Text(
                        text = due.text,
                        style = tokens.mono.copy(fontSize = 10.sp, color = due.color),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(due.background)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }

        // Urgent is the only priority the comp marks here. A dot per priority
        // would turn the list into a colour chart.
        if (entry.item.priority == Priority.URGENT) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun StatusPill(name: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(text = name, fontSize = 11.sp, color = Color(0xFFC9C6C2))
    }
}

@Composable
private fun TodayEmpty(modifier: Modifier = Modifier) {
    val tokens = LocalProjectMateTokens.current
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // An outlined diamond, not a tick. "Done" would be a lie — this says
        // "clear", which is a different and more honest thing.
        Box(
            Modifier
                .padding(bottom = 18.dp)
                .size(44.dp)
                .rotate(45f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                ),
        )
        Text(
            text = "Nothing needs you today",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Overdue work and anything due today lands here, " +
                "along with whatever you've marked as in focus.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = tokens.mono.color,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// ------------------------------------------------------------------- labels

private fun FocusReason.title(): String = when (this) {
    FocusReason.OVERDUE -> "Overdue"
    FocusReason.DUE_TODAY -> "Due today"
    FocusReason.IN_FOCUS_STATUS -> "In focus"
}

private fun FocusReason.color(): Color = when (this) {
    FocusReason.OVERDUE -> Color(0xFFFF7A70)
    FocusReason.DUE_TODAY -> Color(0xFFFF6B1A)
    FocusReason.IN_FOCUS_STATUS -> Color(0xFFC9C6C2)
}

private data class DueLabel(val text: String, val color: Color, val background: Color)

/**
 * The due chip, or null when there's no due date.
 *
 * Overdue and today get colour; anything further out is stated plainly, because
 * a chip that shouts about next Tuesday trains you to ignore the ones that
 * matter.
 */
private fun dueLabel(entry: FocusEntry, zone: ZoneId): DueLabel? {
    val dueAt = entry.item.dueAt ?: return null
    val date = LocalDate.ofInstant(Instant.ofEpochMilli(dueAt), zone)
    val short = date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

    val time = if (entry.item.dueHasTime) {
        " " + Instant.ofEpochMilli(dueAt).atZone(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    } else {
        ""
    }

    return when (entry.bucket) {
        DueBucket.OVERDUE -> DueLabel(
            text = "Overdue · $short",
            color = Color(0xFFFF7A70),
            background = Color(0xFFE2453C).copy(alpha = 0.16f),
        )

        DueBucket.TODAY -> DueLabel(
            text = "Today$time",
            color = Color(0xFFFF6B1A),
            background = Color(0xFFFF6B1A).copy(alpha = 0.14f),
        )

        DueBucket.TOMORROW -> plain("Tomorrow$time")

        DueBucket.THIS_WEEK -> plain(
            date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())) + time,
        )

        DueBucket.LATER -> plain(short + time)

        DueBucket.NONE -> null
    }
}

private fun plain(text: String) = DueLabel(
    text = text,
    color = Color(0xFFC9C6C2),
    background = Color.White.copy(alpha = 0.06f),
)

// ------------------------------------------------------------------ preview

@Preview(showBackground = true, widthDp = 380, backgroundColor = 0xFF0B0B0C)
@Composable
private fun TodayEmptyPreview() {
    ProjectMateTheme {
        TodayScreen(
            state = TodayUiState(isLoading = false),
            onOpenItem = {},
        )
    }
}
