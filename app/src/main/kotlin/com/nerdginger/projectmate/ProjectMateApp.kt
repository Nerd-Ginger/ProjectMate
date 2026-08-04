package com.nerdginger.projectmate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerdginger.projectmate.designsystem.LocalProjectMateTokens
import com.nerdginger.projectmate.di.AppContainer
import com.nerdginger.projectmate.feature.board.BoardDetailScreen
import com.nerdginger.projectmate.feature.board.BoardDetailViewModel
import com.nerdginger.projectmate.feature.boards.BoardTemplateSheet
import com.nerdginger.projectmate.feature.boards.BoardsScreen
import com.nerdginger.projectmate.feature.boards.BoardsViewModel
import com.nerdginger.projectmate.nav.Navigator
import com.nerdginger.projectmate.nav.Screen
import com.nerdginger.projectmate.nav.rememberNavigator

/**
 * The app shell: bottom bar, back handling, and whichever screen is current.
 *
 * A plain [Scaffold] with a [NavigationBar] rather than the adaptive
 * navigation suite. The adaptive API is still experimental, and none of it can
 * be compiled or previewed in this environment — a rail on tablets is not
 * worth another unverifiable dependency while the build is young. See
 * docs/DECISIONS.md D-006.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMateApp(container: AppContainer) {
    val navigator = rememberNavigator()
    val snackbarHostState = remember { SnackbarHostState() }

    // System back pops our own stack; only falls through to the OS at the root.
    BackHandler(enabled = !navigator.isAtRoot) { navigator.pop() }

    val boardsViewModel: BoardsViewModel = viewModel(factory = BoardsViewModel.factory(container))
    val boardsState by boardsViewModel.uiState.collectAsStateWithLifecycle()
    var showTemplateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(boardsState.errorMessage) {
        boardsState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            boardsViewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // A board's own name, not the generic "Board" its Screen carries. The
        // summaries are already loaded here, so no extra query is needed to
        // answer "which board am I looking at?".
        topBar = {
            val screen = navigator.current
            ProjectMateTopBar(
                navigator = navigator,
                title = when (screen) {
                    is Screen.BoardDetail ->
                        boardsState.allBoards
                            .firstOrNull { it.board.id == screen.boardId }
                            ?.board?.name
                            ?: screen.title()

                    else -> screen.title()
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                Navigator.TOP_LEVEL.forEach { destination ->
                    NavigationBarItem(
                        selected = navigator.currentTab == destination,
                        onClick = { navigator.selectTab(destination) },
                        icon = { Icon(destination.icon(), contentDescription = null) },
                        label = { Text(destination.title()) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (navigator.current is Screen.Boards) {
                FloatingActionButton(
                    onClick = { showTemplateSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(19.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New board")
                }
            }
        },
    ) { insets ->
        when (val screen = navigator.current) {
            is Screen.Boards -> BoardsScreen(
                state = boardsState,
                onOpenBoard = { navigator.push(Screen.BoardDetail(it)) },
                contentPadding = insets,
            )

            is Screen.BoardDetail -> BoardDetail(
                container = container,
                boardId = screen.boardId,
                onOpenItem = { navigator.push(Screen.ItemDetail(it)) },
                contentPadding = insets,
            )

            else -> ComingSoon(
                label = screen.title(),
                modifier = Modifier.padding(insets),
            )
        }

        if (showTemplateSheet) {
            BoardTemplateSheet(
                templates = boardsState.templates,
                onDismiss = { showTemplateSheet = false },
                onCreate = { template, name ->
                    boardsViewModel.createBoard(template, name)
                    showTemplateSheet = false
                },
            )
        }
    }
}

/**
 * Two headers in one, because the comp uses two.
 *
 * A top-level destination gets the wordmark eyebrow above a large title — the
 * app announcing itself. A pushed screen gets a compact row with a back arrow,
 * because by then you know what app you're in and the vertical space is better
 * spent on content.
 */
@Composable
private fun ProjectMateTopBar(navigator: Navigator, title: String) {
    val tokens = LocalProjectMateTokens.current
    val screen = navigator.current
    val atTopLevel = screen is Screen.TopLevel

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = if (atTopLevel) 14.dp else 6.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!atTopLevel) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            Column(Modifier.weight(1f)) {
                if (atTopLevel) {
                    Text(
                        text = "ProjectMate".uppercase(),
                        style = tokens.sectionLabel.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Text(
                    text = title,
                    style = if (atTopLevel) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            IconButton(onClick = { /* Overflow menu — docs/SITEMAP.md, not built yet. */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Hosts the kanban view.
 *
 * Keyed by board id so opening a different board gets its own ViewModel
 * rather than briefly showing the previous board's columns.
 */
@Composable
private fun BoardDetail(
    container: AppContainer,
    boardId: String,
    onOpenItem: (String) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    val viewModel: BoardDetailViewModel = viewModel(
        key = "board-$boardId",
        factory = BoardDetailViewModel.factory(container, boardId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BoardDetailScreen(
        state = state,
        onAddItem = viewModel::addItem,
        onAdvance = viewModel::advance,
        onOpenItem = onOpenItem,
        contentPadding = contentPadding,
    )
}

/**
 * Placeholder for screens specified in docs/SITEMAP.md but not yet built.
 *
 * Named rather than blank so it's obvious at a glance which parts of the app
 * are real when a build lands on a phone.
 */
@Composable
private fun ComingSoon(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Not built yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Screen.title(): String = when (this) {
    is Screen.Boards -> "Boards"
    is Screen.Today -> "Today"
    is Screen.Inbox -> "Inbox"
    is Screen.Search -> "Search"
    is Screen.BoardDetail -> "Board"
    is Screen.ItemDetail -> "Item"
    is Screen.StatusEditor -> "Statuses"
    is Screen.BoardSettings -> "Board settings"
    is Screen.SavedView -> "View"
    is Screen.ImportPreview -> "Import"
    is Screen.TagManager -> "Tags"
    is Screen.Archive -> "Archive"
    is Screen.Settings -> "Settings"
}

private fun Screen.TopLevel.icon(): ImageVector = when (this) {
    is Screen.Boards -> Icons.AutoMirrored.Filled.List
    is Screen.Today -> Icons.Default.DateRange
    is Screen.Inbox -> Icons.Default.Email
    is Screen.Search -> Icons.Default.Search
}
