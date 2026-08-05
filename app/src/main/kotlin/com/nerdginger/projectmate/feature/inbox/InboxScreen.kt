package com.nerdginger.projectmate.feature.inbox

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.inbox.InboxGroup
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens

/**
 * Capture and triage.
 *
 * Rows are not tappable-to-open: on this screen the useful actions are triage
 * actions, and they sit on the card. Opening an item is what the board is for.
 */
@Composable
fun InboxScreen(
    state: InboxUiState,
    onToggleSelecting: () -> Unit,
    onToggleSelected: (String) -> Unit,
    onMoveOne: (Item) -> Unit,
    onDismissOne: (Item) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (state.isEmpty) {
        InboxEmpty(modifier.padding(contentPadding))
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        // The comp puts this in the page header; the header here is the app
        // shell's and has no access to this screen's state, so it sits with the
        // list it controls.
        item(key = "select-toggle") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                CardAction(
                    text = if (state.isSelecting) "Done" else "Select",
                    emphasised = state.isSelecting,
                    onClick = onToggleSelecting,
                )
            }
        }

        state.groups.forEach { group ->
            item(key = "header-${group.origin}") { GroupHeader(group) }
            items(group.items, key = { it.id }) { row ->
                InboxCard(
                    item = row,
                    badge = group.badge,
                    meta = state.requestMeta[row.id],
                    isSelecting = state.isSelecting,
                    isSelected = row.id in state.selected,
                    onToggleSelected = { onToggleSelected(row.id) },
                    onMove = { onMoveOne(row) },
                    onDismiss = { onDismissOne(row) },
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(group: InboxGroup) {
    val tokens = LocalProjectMateTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Text(group.label.uppercase(), style = tokens.sectionLabel)
        Text(group.count.toString(), style = tokens.mono.copy(fontSize = 10.sp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(tokens.divider),
        )
    }
}

@Composable
private fun InboxCard(
    item: Item,
    badge: String,
    meta: com.nerdginger.projectmate.core.model.FeatureRequestMeta?,
    isSelecting: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onMove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalProjectMateTokens.current
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = if (isSelected) primary.copy(alpha = 0.5f) else tokens.cardBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(13.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            if (isSelecting) {
                SelectBox(isSelected, onToggleSelected, Modifier.padding(top = 1.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                item.notes?.takeIf { it.isNotBlank() }?.let { body ->
                    Text(
                        text = body,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = tokens.mono.color,
                        modifier = Modifier.padding(top = 5.dp),
                        maxLines = 3,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = badge,
                        style = tokens.mono.copy(fontSize = 10.sp, color = Color(0xFF93908C)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.dp, tokens.chipBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                    meta?.let {
                        val who = it.requesterName ?: "anonymous"
                        val votes = if (it.votes > 0) "  ·  ${it.votes} votes" else ""
                        Text("$who$votes", style = tokens.mono.copy(fontSize = 10.sp))
                    }
                }
            }
        }

        // Hidden in bulk mode — the action bar at the bottom takes over, and
        // per-row buttons would make it ambiguous what a tap applies to.
        if (!isSelecting) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                CardAction(
                    text = "Move to board",
                    modifier = Modifier.weight(1f),
                    emphasised = true,
                    onClick = onMove,
                )
                CardAction(text = "Dismiss", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun SelectBox(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) primary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) primary else Color(0xFF5C5C64),
                shape = RoundedCornerShape(5.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                "✓",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun CardAction(
    text: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (emphasised) {
                    primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (emphasised) primary else Color(0xFFC9C6C2),
        )
    }
}

/**
 * The bulk action bar, floating above the bottom bar while selecting.
 *
 * Rendered by the shell rather than the screen, so it sits over the list rather
 * than scrolling with it.
 */
@Composable
fun InboxBulkBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalProjectMateTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, tokens.chipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$selectedCount selected",
            style = tokens.mono.copy(fontSize = 11.sp, color = Color(0xFF93908C)),
            modifier = Modifier.weight(1f),
        )
        CardAction(text = "Move", emphasised = true, onClick = onMove)
        CardAction(text = "Dismiss", onClick = onDismiss)
    }
}

@Composable
private fun InboxEmpty(modifier: Modifier = Modifier) {
    val tokens = LocalProjectMateTokens.current
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Inbox clear",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Everything captured has a board.",
            fontSize = 13.sp,
            color = tokens.mono.color,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
