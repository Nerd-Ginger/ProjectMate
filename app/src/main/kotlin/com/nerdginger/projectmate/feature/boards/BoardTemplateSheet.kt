package com.nerdginger.projectmate.feature.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nerdginger.projectmate.core.template.BoardTemplate
import com.nerdginger.projectmate.core.template.BoardTemplates
import com.nerdginger.projectmate.designsystem.ProjectMateTheme
import com.nerdginger.projectmate.designsystem.component.indicatorColor

/**
 * Create a board from a template.
 *
 * A sheet rather than a screen, so the board list stays visible behind it —
 * see docs/SITEMAP.md.
 *
 * Templates are a starting point, not a contract: the statuses they seed are
 * fully editable afterwards, which is why the sheet shows them up front. You
 * should be able to see what you're getting before you commit to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardTemplateSheet(
    templates: List<BoardTemplate>,
    onDismiss: () -> Unit,
    onCreate: (BoardTemplate, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(templates.firstOrNull()) }
    var name by remember { mutableStateOf("") }

    // Pre-fill the name from the template so the common case is one tap.
    LaunchedEffect(selected) {
        name = selected?.name.orEmpty()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New board", style = MaterialTheme.typography.titleLarge)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = template.id == selected?.id,
                        onClick = { selected = template },
                    )
                }
            }

            selected?.let { template ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCreate(template, name) }),
                )

                StatusPreview(template)

                Button(
                    onClick = { onCreate(template, name) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create board")
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: BoardTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(template.emoji, style = MaterialTheme.typography.titleLarge)
            Text(
                text = template.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/**
 * The statuses this template will create, with their category colours.
 *
 * Shown because the status set is the only real difference between templates —
 * a name and an emoji tell you nothing about how the board will behave.
 */
@Composable
private fun StatusPreview(template: BoardTemplate) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = template.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        template.statuses.forEach { status ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = Color(status.colorArgb),
                ) {}
                Text(status.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = status.category.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = status.category.indicatorColor(),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun StatusPreviewPreview() {
    ProjectMateTheme(dynamicColor = false) {
        Column(Modifier.padding(20.dp)) {
            StatusPreview(BoardTemplates.Projects)
        }
    }
}
