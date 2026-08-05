package com.nerdginger.projectmate.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import com.nerdginger.projectmate.designsystem.component.BoardAvatar

/**
 * Where should this go?
 *
 * Triage lands an item on the board's **default status**, not a status you pick
 * — one decision instead of two. Getting it to the right board is the part that
 * matters; the column can be adjusted on the board itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardPickerSheet(
    boards: List<Board>,
    onDismiss: () -> Unit,
    onPick: (Board) -> Unit,
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
            Text(
                text = "Move to board",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            if (boards.isEmpty()) {
                Text(
                    text = "No boards yet — create one first.",
                    fontSize = 13.sp,
                    color = tokens.mono.color,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                return@Column
            }

            boards.forEach { board ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPick(board) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BoardAvatar(
                        name = board.name,
                        accent = Color(board.accentColor),
                        size = 28.dp,
                    )
                    Text(
                        text = board.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
