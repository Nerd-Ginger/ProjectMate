package com.nerdginger.projectmate.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens

/**
 * Everything, filtered.
 *
 * No page title — the field is the header, per the comp. Results are grouped by
 * board and deliberately compact: one line each, because a search result is
 * something you scan and tap, not something you read.
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onToggleFilter: (SearchFilter) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        QueryField(state.query, onQueryChange)
        FilterRow(state.filters, onToggleFilter)

        when {
            state.isIdle -> Hint("Search titles and notes across every board.")
            state.isEmpty -> Hint("Nothing matches.")
            else -> Results(state, onOpenItem, contentPadding)
        }
    }
}

@Composable
private fun QueryField(query: String, onChange: (String) -> Unit) {
    val tokens = LocalProjectMateTokens.current
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, tokens.chipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The comp draws the magnifier as a bare ring; so does this.
        Box(
            Modifier
                .size(13.dp)
                .clip(RoundedCornerShape(7.dp))
                .border(1.5.dp, Color(0xFF93908C), RoundedCornerShape(7.dp)),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Title and notes", fontSize = 15.sp, color = Color(0xFF7E7B77))
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FilterRow(active: Set<SearchFilter>, onToggle: (SearchFilter) -> Unit) {
    val tokens = LocalProjectMateTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SearchFilter.entries.forEach { filter ->
            val isOn = filter in active
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isOn) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isOn) MaterialTheme.colorScheme.primary else tokens.chipBorder,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onToggle(filter) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = filter.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOn) MaterialTheme.colorScheme.primary else Color(0xFFC9C6C2),
                )
            }
        }
    }
}

@Composable
private fun Results(
    state: SearchUiState,
    onOpenItem: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val tokens = LocalProjectMateTokens.current

    LazyColumn(
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        item(key = "summary") {
            Text(
                text = state.summary,
                style = tokens.mono.copy(fontSize = 10.sp, color = Color(0xFF6E6B67)),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )
        }

        state.groups.forEach { group ->
            item(key = "header-${group.board?.id ?: "none"}") {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                group.board?.let { Color(it.accentColor) }
                                    ?: MaterialTheme.colorScheme.primary,
                            ),
                    )
                    Text(
                        text = group.board?.name ?: "Unknown board",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC9C6C2),
                    )
                    Text(
                        text = group.count.toString(),
                        style = tokens.mono.copy(fontSize = 10.sp, color = Color(0xFF6E6B67)),
                    )
                }
            }

            items(group.items, key = { it.id }) { row ->
                ResultRow(
                    item = row,
                    statusColor = group.statusesById[row.statusId]
                        ?.let { Color(it.colorArgb) }
                        ?: Color(0xFF5C5C64),
                    onClick = { onOpenItem(row.id) },
                )
            }

            item(key = "gap-${group.board?.id ?: "none"}") {
                Box(Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ResultRow(item: Item, statusColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(statusColor),
        )
        Text(
            text = item.title,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Shown before you've typed anything, and when nothing matches.
 *
 * The comp has neither — it would leave a bare `0 items` and a blank screen,
 * which reads as broken rather than as an answer.
 */
@Composable
private fun Hint(text: String) {
    val tokens = LocalProjectMateTokens.current
    Box(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = tokens.mono.color,
            textAlign = TextAlign.Center,
        )
    }
}
