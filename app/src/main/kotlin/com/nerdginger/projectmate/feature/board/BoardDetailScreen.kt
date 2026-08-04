package com.nerdginger.projectmate.feature.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
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
    val tokens = LocalProjectMateTokens.current
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(280.dp)
            // Full height, so the add field sits at the bottom of a tall column
            // rather than the card shrink-wrapping around whatever is in it.
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColumnHeader(column)

        LazyColumn(
            modifier = Modifier.weight(1f),
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
            placeholder = { Text("Add item", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = tokens.chipBorder,
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.primary,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
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
    val tokens = LocalProjectMateTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // A small rounded square, not a dot — matches the swatch shape used in
        // the progress-bar legend so the same colour reads as the same thing.
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(column.status.colorArgb)),
        )

        Text(
            text = column.status.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = column.status.wipLimit?.let { "${column.items.size}/$it" }
                ?: "${column.items.size}",
            // A WIP limit is a nudge, not a rule — nothing is prevented, the
            // count just stops looking calm about it.
            style = tokens.mono.copy(
                color = if (column.isOverWipLimit) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    tokens.mono.color
                },
            ),
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
    val tokens = LocalProjectMateTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
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
            // The comp's advance affordance: a small orange-tinted tile, not a
            // bare icon button. One tap is the common case, so it gets colour.
            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAdvance),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Move to next status",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Null for [Priority.NONE] — most items have no priority and need no mark.
 *
 * Urgent borrows the blocked red; the palette has exactly one alarm colour and
 * two would dilute it.
 */
@Composable
private fun Priority.indicator(): Color? {
    val tokens = LocalProjectMateTokens.current
    return when (this) {
        Priority.NONE -> null
        Priority.LOW -> tokens.done
        Priority.NORMAL -> tokens.accents[0]
        Priority.URGENT -> tokens.blocked
    }
}
