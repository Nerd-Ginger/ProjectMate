package com.nerdginger.projectmate.feature.board

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Priority

/**
 * The kanban view: one column per status, in the board's own order.
 *
 * v1 moves cards with an "advance" button rather than drag-and-drop. Moving a
 * card one column to the right is the overwhelmingly common action, and a
 * single tap beats a drag for it. Ordering already uses fractional sort keys,
 * so real drag-and-drop drops in later with no schema change — see
 * docs/DECISIONS.md D-009.
 */
@Composable
fun BoardDetailScreen(
    state: BoardDetailUiState,
    onAddItem: (statusId: String, title: String) -> Unit,
    onAdvance: (Item) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (state.board == null) {
        Box(modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text(
                text = if (state.isLoading) "" else "This board no longer exists.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyRow(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.columns, key = { it.status.id }) { column ->
            StatusColumn(
                column = column,
                isLastColumn = column.status.id == state.columns.lastOrNull()?.status?.id,
                onAddItem = { title -> onAddItem(column.status.id, title) },
                onAdvance = onAdvance,
                onOpenItem = onOpenItem,
            )
        }
    }
}

@Composable
private fun StatusColumn(
    column: Column,
    isLastColumn: Boolean,
    onAddItem: (String) -> Unit,
    onAdvance: (Item) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.width(280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColumnHeader(column)

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(column.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    canAdvance = !isLastColumn,
                    onAdvance = { onAdvance(item) },
                    onClick = { onOpenItem(item.id) },
                )
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text("Add…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onAddItem(draft)
                    draft = ""
                },
            ),
        )
    }
}

@Composable
private fun ColumnHeader(column: Column) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = Color(column.status.colorArgb),
        ) {}

        Text(
            text = column.status.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = column.status.wipLimit?.let { "${column.items.size}/$it" }
                ?: "${column.items.size}",
            style = MaterialTheme.typography.labelMedium,
            // A WIP limit is a nudge, not a rule — nothing is prevented, the
            // count just stops looking calm about it.
            color = if (column.isOverWipLimit) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ItemCard(
    item: Item,
    canAdvance: Boolean,
    onAdvance: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item.priority.indicator()?.let { color ->
                Box(
                    Modifier
                        .size(width = 3.dp, height = 28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            if (canAdvance) {
                IconButton(onClick = onAdvance, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move to next status",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Null for [Priority.NONE] — most items have no priority and need no mark. */
private fun Priority.indicator(): Color? = when (this) {
    Priority.NONE -> null
    Priority.LOW -> Color(0xFF94A3B8)
    Priority.NORMAL -> Color(0xFF3B82F6)
    Priority.URGENT -> Color(0xFFEF4444)
}
