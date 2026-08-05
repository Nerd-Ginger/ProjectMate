package com.nerdginger.projectmate.feature.boards

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.BoardProgress
import com.nerdginger.projectmate.core.model.BoardType
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.model.SyncMeta
import com.nerdginger.projectmate.data.repository.BoardSummary
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import com.nerdginger.projectmate.designsystem.ProjectMateTheme
import com.nerdginger.projectmate.designsystem.component.BoardAvatar
import com.nerdginger.projectmate.designsystem.component.BoardProgressBar
import com.nerdginger.projectmate.designsystem.component.BoardProgressLegend

/**
 * The collections list — the answer to "what am I tracking?".
 *
 * Stateless: everything comes in as [BoardsUiState] and goes out as lambdas,
 * so it can be previewed and, later, tested without a database.
 */
@Composable
fun BoardsScreen(
    state: BoardsUiState,
    onOpenBoard: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (state.isEmpty) {
        EmptyBoards(modifier.padding(contentPadding))
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (state.pinned.isNotEmpty()) {
            item { SectionHeader("Pinned") }
            items(state.pinned, key = { it.board.id }) { summary ->
                BoardCard(summary, onClick = { onOpenBoard(summary.board.id) })
            }
        }

        if (state.others.isNotEmpty()) {
            item { SectionHeader("Boards") }
            items(state.others, key = { it.board.id }) { summary ->
                BoardCard(summary, onClick = { onOpenBoard(summary.board.id) })
            }
        }
    }
}

/**
 * A wide-tracked monospace label with a hairline rule running to the edge —
 * the comp's way of separating groups without a heavy divider.
 */
@Composable
private fun SectionHeader(text: String) {
    val tokens = LocalProjectMateTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Text(text.uppercase(), style = tokens.sectionLabel)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(tokens.divider),
        )
    }
}

@Composable
private fun BoardCard(
    summary: BoardSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalProjectMateTokens.current
    val board = summary.board
    val progress = summary.progress
    val accent = Color(board.accentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoardAvatar(board.name, accent)

            Column(Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summaryLine(progress),
                    style = tokens.mono,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // A pinned board gets the accent diamond the comp puts top-right.
            if (board.isPinned) {
                Box(
                    Modifier
                        .size(8.dp)
                        .rotate(45f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        if (progress.total > 0) {
            BoardProgressBar(progress, Modifier.padding(top = 13.dp))
            BoardProgressLegend(progress, Modifier.padding(top = 9.dp))
        }
    }
}

/**
 * The one-line summary under a board's name.
 *
 * Says how much is still open — the comp's "5 open". The per-category detail
 * lives in the legend immediately below, so repeating it here would be noise.
 */
private fun summaryLine(progress: BoardProgress): String {
    if (progress.total == 0) return "nothing here yet"

    val cancelled = progress.countsByCategory[StatusCategory.CANCELLED] ?: 0
    val open = progress.total - progress.completed - cancelled
    return if (open > 0) "$open open" else "all done"
}

@Composable
private fun EmptyBoards(modifier: Modifier = Modifier) {
    val tokens = LocalProjectMateTokens.current
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No boards yet", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Create one from a template — Projects for things you're building, " +
                "Life for everything else.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.mono.color,
            textAlign = TextAlign.Center,
        )
    }
}

// ------------------------------------------------------------------ previews

private fun previewBoard(
    id: String,
    name: String,
    emoji: String,
    color: Int,
    pinned: Boolean = false,
) = Board(
    id = id,
    name = name,
    emoji = emoji,
    accentColor = color,
    boardType = BoardType.CUSTOM,
    sortKey = "a0",
    isPinned = pinned,
    sync = SyncMeta(createdAt = 0, updatedAt = 0),
)

@Preview(showBackground = true, widthDp = 380, backgroundColor = 0xFF0B0B0C)
@Composable
private fun BoardsScreenPreview() {
    ProjectMateTheme {
        BoardsScreen(
            state = BoardsUiState(
                isLoading = false,
                pinned = listOf(
                    BoardSummary(
                        previewBoard("1", "Portal v2", "📨", 0xFF4A8FE7.toInt(), pinned = true),
                        BoardProgress(
                            mapOf(
                                StatusCategory.BACKLOG to 2,
                                StatusCategory.ACTIVE to 2,
                                StatusCategory.BLOCKED to 1,
                                StatusCategory.DONE to 1,
                            ),
                        ),
                    ),
                ),
                others = listOf(
                    BoardSummary(
                        previewBoard("2", "Feature requests", "🛠️", 0xFFD9628E.toInt()),
                        BoardProgress(
                            mapOf(
                                StatusCategory.BACKLOG to 2,
                                StatusCategory.ACTIVE to 1,
                                StatusCategory.DONE to 3,
                            ),
                        ),
                    ),
                    BoardSummary(
                        previewBoard("3", "House", "🌱", 0xFF2E9E8F.toInt()),
                        BoardProgress(mapOf(StatusCategory.ACTIVE to 2, StatusCategory.DONE to 9)),
                    ),
                ),
            ),
            onOpenBoard = {},
        )
    }
}
