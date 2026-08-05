package com.nerdginger.projectmate.feature.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.Tag
import com.nerdginger.projectmate.core.time.DueDates
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The sheets item detail opens.
 *
 * Sheets rather than screens because none of them should cost you your place —
 * see the nav rules in docs/SITEMAP.md.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerSheet(
    statuses: List<Status>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onPick: (Status) -> Unit,
) {
    val tokens = LocalProjectMateTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            SheetTitle("Status")

            statuses.forEach { status ->
                val isSelected = status.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable { onPick(status) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(status.colorArgb)),
                    )
                    Text(
                        text = status.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    // The category is the thing that actually drives behaviour,
                    // so it's worth showing while you're choosing.
                    Text(
                        text = status.category.name.lowercase(),
                        style = tokens.mono.copy(fontSize = 10.sp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuePickerSheet(
    initialDueAt: Long?,
    onDismiss: () -> Unit,
    onPick: (dueAt: Long?, hasTime: Boolean) -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDueAt)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Clearing has to be as easy as setting. A due date you can't
                // remove is worse than one you never set.
                SheetButton(
                    text = "Clear",
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = { onPick(null, false) },
                )
                SheetButton(
                    text = "Set date",
                    filled = true,
                    modifier = Modifier.weight(1f),
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        onPick(pickerState.selectedDateMillis?.toLocalAllDay(), false)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerSheet(
    attached: List<Tag>,
    all: List<Tag>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Tag) -> Unit,
) {
    val tokens = LocalProjectMateTokens.current
    var draft by remember { mutableStateOf("") }
    val attachedIds = attached.map { it.id }.toSet()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            SheetTitle("Tags")

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("New tag", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.chipBorder,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onAdd(draft)
                        draft = ""
                    },
                ),
            )

            if (attached.isNotEmpty()) {
                Text(
                    "On this item".uppercase(),
                    style = tokens.sectionLabel,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                TagChips(attached) { onRemove(it) }
            }

            val available = all.filter { it.id !in attachedIds }
            if (available.isNotEmpty()) {
                Text(
                    "Existing".uppercase(),
                    style = tokens.sectionLabel,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                TagChips(available) { onAdd(it.name) }
            }
        }
    }
}

/**
 * Turns the date picker's answer into an all-day timestamp in the local zone.
 *
 * **Material's `DatePicker` reports the selected day as midnight UTC.** Stored
 * raw and read back in a zone behind UTC, that is the *previous* day — picking
 * 1 August on a UTC−4 device saved 31 July. Verified on a device in
 * America/New_York; a compile would never have shown it.
 *
 * [DueDates.allDayOn] is the existing fix: it normalises a calendar date to the
 * start of that day where the user actually is, which is what "all-day" has to
 * mean for overdue to behave.
 */
private fun Long.toLocalAllDay(): Long =
    DueDates.allDayOn(
        date = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate(),
        zone = ZoneId.systemDefault(),
    )

@Composable
private fun TagChips(tags: List<Tag>, onClick: (Tag) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onClick(tag) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
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
private fun SheetTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

@Composable
private fun SheetButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tokens = LocalProjectMateTokens.current
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) primary else Color.Transparent)
            .then(
                if (filled) Modifier else Modifier.border(1.dp, tokens.chipBorder, RoundedCornerShape(10.dp)),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                filled -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            }.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}
