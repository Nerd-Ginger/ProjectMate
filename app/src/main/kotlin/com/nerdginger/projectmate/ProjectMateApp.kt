package com.nerdginger.projectmate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.nerdginger.projectmate.di.AppContainer
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
        topBar = { TopAppBar(title = { Text(navigator.current.title()) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Navigator.TOP_LEVEL.forEach { destination ->
                    NavigationBarItem(
                        selected = navigator.currentTab == destination,
                        onClick = { navigator.selectTab(destination) },
                        icon = { Icon(destination.icon(), contentDescription = null) },
                        label = { Text(destination.title()) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (navigator.current is Screen.Boards) {
                FloatingActionButton(onClick = { showTemplateSheet = true }) {
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
