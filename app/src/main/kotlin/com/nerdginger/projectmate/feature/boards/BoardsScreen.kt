package com.nerdginger.projectmate.feature.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.BoardProgress
import com.nerdginger.projectmate.core.model.BoardType
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.model.SyncMeta
import com.nerdginger.projectmate.data.repository.BoardSummary
import com.nerdginger.projectmate.designsystem.ProjectMateTheme
import com.nerdginger.projectmate.designsystem.component.BoardProgressBar

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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun BoardCard(
    summary: BoardSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val board = summary.board
    val progress = summary.progress

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                board.emoji?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = board.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(board.accentColor),
                    )
                    Text(
                        text = summaryLine(progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            BoardProgressBar(progress)
        }
    }
}

/**
 * The one-line summary under a board's name.
 *
 * Leads with what's blocked when anything is: a stalled board is the thing
 * most worth noticing from the home screen, and it's exactly what a single
 * completion percentage would hide.
 */
private fun summaryLine(progress: BoardProgress): String {
    if (progress.total == 0) return "Nothing here yet"

    val parts = buildList {
        if (progress.blocked > 0) add("${progress.blocked} blocked")
        val active = progress.countsByCategory[StatusCategory.ACTIVE] ?: 0
        if (active > 0) add("$active active")
        add("${progress.completed}/${progress.total - (progress.countsByCategory[StatusCategory.CANCELLED] ?: 0)} done")
    }
    return parts.joinToString(" · ")
}

@Composable
private fun EmptyBoards(modifier: Modifier = Modifier) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun BoardsScreenPreview() {
    ProjectMateTheme(dynamicColor = false) {
        BoardsScreen(
            state = BoardsUiState(
                isLoading = false,
                pinned = listOf(
                    BoardSummary(
                        previewBoard("1", "Inbox", "📨", 0xFFF59E0B.toInt(), pinned = true),
                        BoardProgress(mapOf(StatusCategory.INBOX to 3)),
                    ),
                ),
                others = listOf(
                    BoardSummary(
                        previewBoard("2", "Projects", "🛠️", 0xFF6366F1.toInt()),
                        BoardProgress(
                            mapOf(
                                StatusCategory.BACKLOG to 4,
                                StatusCategory.ACTIVE to 2,
                                StatusCategory.BLOCKED to 1,
                                StatusCategory.DONE to 5,
                            ),
                        ),
                    ),
                    BoardSummary(
                        previewBoard("3", "Life", "🌱", 0xFF14B8A6.toInt()),
                        BoardProgress(mapOf(StatusCategory.ACTIVE to 2, StatusCategory.DONE to 9)),
                    ),
                ),
            ),
            onOpenBoard = {},
        )
    }
}
