package com.nerdginger.projectmate.feature.item

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.model.ChecklistEntry
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.Tag
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One item, everything about it, editable in place.
 *
 * Autosaves on every change — there is no save button and no destructive back,
 * per docs/SITEMAP.md. Text fields commit when focus leaves them rather than on
 * every keystroke, so a half-typed title never lands in the database.
 */
@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onPickStatus: () -> Unit,
    onPickDue: () -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
    onAddChecklistEntry: (String) -> Unit,
    onToggleChecklistEntry: (ChecklistEntry, Boolean) -> Unit,
    onDeleteChecklistEntry: (ChecklistEntry) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val tokens = LocalProjectMateTokens.current
    val item = state.item

    if (item == null) {
        Box(
            modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (state.isMissing) "This item no longer exists." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.mono.color,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
    ) {
        TitleField(item.title, onTitleChange)

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            state.status?.let { StatusChip(it, onPickStatus) }
            DueChip(item.dueAt, item.dueHasTime, onPickDue)
        }

        SectionLabel("Priority", Modifier.padding(top = 22.dp))
        PriorityRow(item.priority, onPriorityChange)

        SectionLabel(
            text = "Tags",
            modifier = Modifier.padding(top = 22.dp),
            action = "Edit" to onAddTag,
        )
        TagRow(state.tags, onAddTag, onRemoveTag)

        SectionLabel("Notes", Modifier.padding(top = 22.dp))
        NotesField(item.notes.orEmpty(), onNotesChange)

        SectionLabel(
            text = "Checklist",
            modifier = Modifier.padding(top = 22.dp),
            trailing = if (state.checklist.isEmpty()) {
                "empty"
            } else {
                "${state.checklistDone} of ${state.checklist.size}"
            },
        )
        ChecklistSection(
            entries = state.checklist,
            fraction = state.checklistFraction,
            onToggle = onToggleChecklistEntry,
            onDelete = onDeleteChecklistEntry,
            onAdd = onAddChecklistEntry,
        )

        state.featureRequest?.let { FeatureRequestCard(it) }

        // Only shown when the item didn't come from this device — a "source"
        // line on something you typed yourself is noise.
        if (item.sync.origin.needsTriage) {
            Text(
                text = "source: ${item.sync.origin.name.lowercase().replace('_', ' ')}",
                style = tokens.mono.copy(fontSize = 10.sp, color = Color(0xFF5E5B57)),
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

/**
 * Formatters are built once, outside any composable.
 *
 * Reading the locale inside a composable is flagged by lint
 * (`NonObservableLocale`) because a locale change wouldn't recompose. Hoisting
 * them also avoids rebuilding a formatter on every frame.
 */
private val DAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

private val TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun formatDay(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DAY_FORMAT)

// ------------------------------------------------------------------ sections

@Composable
private fun TitleField(title: String, onCommit: (String) -> Unit) {
    val tokens = LocalProjectMateTokens.current
    var draft by remember(title) { mutableStateOf(title) }

    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 27.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusLost { onCommit(draft) }
            .padding(bottom = 10.dp),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(tokens.chipBorder),
    )
}

@Composable
private fun NotesField(notes: String, onCommit: (String) -> Unit) {
    val tokens = LocalProjectMateTokens.current
    var draft by remember(notes) { mutableStateOf(notes) }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(11.dp))
            .heightIn(min = 76.dp)
            .padding(12.dp),
    ) {
        if (draft.isEmpty()) {
            Text("Markdown", fontSize = 13.5.sp, color = Color(0xFF5E5B57))
        }
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            textStyle = LocalTextStyle.current.copy(
                color = Color(0xFFC9C6C2),
                fontSize = 13.5.sp,
                lineHeight = 21.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().onFocusLost { onCommit(draft) },
        )
    }
}

@Composable
private fun StatusChip(status: Status, onClick: () -> Unit) {
    val color = Color(status.colorArgb)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(
            text = status.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DueChip(dueAt: Long?, hasTime: Boolean, onClick: () -> Unit) {
    val tokens = LocalProjectMateTokens.current
    val zone = ZoneId.systemDefault()

    val (text, color, background) = when {
        dueAt == null -> Triple(
            "no due date",
            Color(0xFF8C8983),
            MaterialTheme.colorScheme.surfaceVariant,
        )

        else -> {
            val date = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
            val today = LocalDate.now(zone)
            val label = formatDay(dueAt) +
                if (hasTime) {
                    " " + Instant.ofEpochMilli(dueAt).atZone(zone).format(TIME_FORMAT)
                } else {
                    "  ·  all-day"
                }

            when {
                date.isBefore(today) -> Triple(
                    label,
                    Color(0xFFFF7A70),
                    Color(0xFFE2453C).copy(alpha = 0.16f),
                )

                date == today -> Triple(
                    label,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                )

                else -> Triple(label, Color(0xFFC9C6C2), MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }

    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = tokens.mono.copy(fontSize = 11.sp, color = color))
    }
}

@Composable
private fun PriorityRow(selected: Priority, onSelect: (Priority) -> Unit) {
    val tokens = LocalProjectMateTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Priority.entries.forEach { priority ->
            val isSelected = priority == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            tokens.chipBorder
                        },
                        shape = RoundedCornerShape(9.dp),
                    )
                    .clickable { onSelect(priority) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = priority.label(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFF8C8983)
                    },
                )
            }
        }
    }
}

@Composable
private fun TagRow(tags: List<Tag>, onAdd: () -> Unit, onRemove: (Tag) -> Unit) {
    if (tags.isEmpty()) {
        Text(
            text = "None yet",
            fontSize = 12.sp,
            color = Color(0xFF5E5B57),
            modifier = Modifier.clickable(onClick = onAdd).padding(vertical = 6.dp),
        )
        return
    }

    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onRemove(tag) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(tag.colorArgb)),
                )
                Text(tag.name, fontSize = 12.sp, color = Color(0xFFC9C6C2))
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    entries: List<ChecklistEntry>,
    fraction: Float,
    onToggle: (ChecklistEntry, Boolean) -> Unit,
    onDelete: (ChecklistEntry) -> Unit,
    onAdd: (String) -> Unit,
) {
    val tokens = LocalProjectMateTokens.current

    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(tokens.track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }

    Column(Modifier.padding(top = 11.dp)) {
        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(entry, !entry.isDone) }
                    .padding(horizontal = 4.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(entry.isDone)
                Text(
                    text = entry.text,
                    fontSize = 13.5.sp,
                    color = if (entry.isDone) Color(0xFF7E7B77) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (entry.isDone) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "✕",
                    fontSize = 12.sp,
                    color = Color(0xFF5E5B57),
                    modifier = Modifier
                        .clickable { onDelete(entry) }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        AddChecklistRow(onAdd)
    }
}

@Composable
private fun Checkbox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(19.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .border(
                width = 1.5.dp,
                color = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color(0xFF4A4A52)
                },
                shape = RoundedCornerShape(5.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                "✓",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun AddChecklistRow(onAdd: (String) -> Unit) {
    val tokens = LocalProjectMateTokens.current
    var draft by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(19.dp)
                .clip(RoundedCornerShape(5.dp))
                .border(1.5.dp, tokens.chipBorder, RoundedCornerShape(5.dp)),
        )
        Box(Modifier.weight(1f)) {
            if (draft.isEmpty()) {
                Text("Add an item", fontSize = 13.5.sp, color = Color(0xFF5E5B57))
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        onAdd(draft)
                        draft = ""
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeatureRequestCard(meta: com.nerdginger.projectmate.core.model.FeatureRequestMeta) {
    val tokens = LocalProjectMateTokens.current
    val primary = MaterialTheme.colorScheme.primary

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(primary.copy(alpha = 0.07f))
            .border(1.dp, primary.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
            .padding(14.dp),
    ) {
        Text("Feature request".uppercase(), style = tokens.sectionLabel.copy(color = primary))

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column {
                Text(
                    text = meta.votes.toString(),
                    style = tokens.mono.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Text(
                    "votes",
                    fontSize = 11.sp,
                    color = tokens.mono.color,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Column {
                Text(
                    text = meta.requesterName ?: "anonymous",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "submitted " + formatDay(meta.submittedAt),
                    fontSize = 11.sp,
                    color = tokens.mono.color,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/**
 * The wide-tracked uppercase label above each section, optionally with a count
 * on the right or a tappable action.
 */
@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    action: Pair<String, () -> Unit>? = null,
) {
    val tokens = LocalProjectMateTokens.current
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = tokens.sectionLabel)
        trailing?.let {
            Text(it, style = tokens.mono.copy(fontSize = 10.sp))
        }
        action?.let { (label, onClick) ->
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick),
            )
        }
    }
}

private fun Priority.label(): String = when (this) {
    Priority.NONE -> "None"
    Priority.LOW -> "Low"
    Priority.NORMAL -> "Normal"
    Priority.URGENT -> "Urgent"
}

/**
 * Runs [onLost] when focus leaves the field.
 *
 * Text commits on focus loss rather than per keystroke: a database write for
 * every character is wasteful, and a half-typed title briefly becoming the
 * item's real title would show up on other screens.
 */
private fun Modifier.onFocusLost(onLost: () -> Unit): Modifier = composed {
    var wasFocused by remember { mutableStateOf(false) }
    onFocusChanged { focusState ->
        if (wasFocused && !focusState.isFocused) onLost()
        wasFocused = focusState.isFocused
    }
}
