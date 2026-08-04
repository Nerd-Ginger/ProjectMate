package com.nerdginger.projectmate.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Every destination in the app.
 *
 * A sealed hierarchy rather than route strings, so arguments are type-safe by
 * construction and a typo is a compile error rather than a crash at runtime.
 */
sealed interface Screen {

    /** The four bottom-bar destinations, each with its own back stack. */
    sealed interface TopLevel : Screen

    data object Boards : TopLevel

    data object Today : TopLevel

    data object Inbox : TopLevel

    data object Search : TopLevel

    data class BoardDetail(val boardId: String) : Screen

    data class ItemDetail(val itemId: String) : Screen

    data class StatusEditor(val boardId: String) : Screen

    data class BoardSettings(val boardId: String) : Screen

    data class SavedView(val viewId: String) : Screen

    /** Import preview. Nothing is written before this screen is confirmed. */
    data class ImportPreview(val payload: String) : Screen

    data object TagManager : Screen

    data object Archive : Screen

    data object Settings : Screen
}

/**
 * The back stack, owned by the app rather than a library.
 *
 * This is deliberately about sixty lines instead of a navigation dependency —
 * see docs/DECISIONS.md D-010. Pushing the import screen from a share intent is
 * `navigator.push(Screen.ImportPreview(json))`: no graph, no route parsing, no
 * argument encoding.
 *
 * Each top-level destination keeps its own stack, so switching tabs never
 * loses your place.
 */
@Stable
class Navigator(
    initial: Screen.TopLevel = Screen.Boards,
    restoredStacks: Map<Screen.TopLevel, List<Screen>>? = null,
    restoredTabHistory: List<Screen.TopLevel>? = null,
) {
    private val stacks: Map<Screen.TopLevel, SnapshotStateList<Screen>> = TOP_LEVEL.associateWith {
        tab ->
        val restored = restoredStacks?.get(tab)
        if (restored.isNullOrEmpty()) {
            mutableStateListOf<Screen>(tab)
        } else {
            mutableStateListOf<Screen>().apply { addAll(restored) }
        }
    }

    private val tabHistory: SnapshotStateList<Screen.TopLevel> =
        if (restoredTabHistory.isNullOrEmpty()) {
            mutableStateListOf(initial)
        } else {
            mutableStateListOf<Screen.TopLevel>().apply { addAll(restoredTabHistory) }
        }

    /** The tab currently shown. */
    val currentTab: Screen.TopLevel get() = tabHistory.last()

    /** The screen currently shown. */
    val current: Screen get() = stackFor(currentTab).last()

    /** True when back would leave the app. */
    val isAtRoot: Boolean
        get() = stackFor(currentTab).size == 1 && tabHistory.size == 1

    private fun stackFor(tab: Screen.TopLevel): SnapshotStateList<Screen> =
        stacks.getValue(tab)

    fun push(screen: Screen) {
        stackFor(currentTab).add(screen)
    }

    /**
     * Selects a tab. Tapping the tab you're already on pops that tab back to
     * its root, which is the behaviour people expect from a bottom bar.
     */
    fun selectTab(tab: Screen.TopLevel) {
        if (tab == currentTab) {
            val stack = stackFor(tab)
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            return
        }
        tabHistory.remove(tab)
        tabHistory.add(tab)
    }

    /** Returns false when there is nothing left to pop. */
    fun pop(): Boolean {
        val stack = stackFor(currentTab)
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        // At a tab root: fall back through previously visited tabs rather than
        // exiting from whichever tab happens to be showing.
        if (tabHistory.size > 1) {
            tabHistory.removeAt(tabHistory.lastIndex)
            return true
        }
        return false
    }

    /** Replaces the current tab's stack, e.g. after confirming an import. */
    fun resetTo(tab: Screen.TopLevel) {
        val stack = stackFor(tab)
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        tabHistory.remove(tab)
        tabHistory.add(tab)
    }

    /** Snapshot of every tab's stack, for [NavigatorSaver]. */
    internal fun snapshot(): Pair<Map<Screen.TopLevel, List<Screen>>, List<Screen.TopLevel>> =
        stacks.mapValues { (_, stack) -> stack.toList() } to tabHistory.toList()

    companion object {
        val TOP_LEVEL: List<Screen.TopLevel> =
            listOf(Screen.Boards, Screen.Today, Screen.Inbox, Screen.Search)
    }
}

// ------------------------------------------------------------------ saving

private fun Screen.encode(): String = when (this) {
    is Screen.Boards -> "boards"
    is Screen.Today -> "today"
    is Screen.Inbox -> "inbox"
    is Screen.Search -> "search"
    is Screen.TagManager -> "tags"
    is Screen.Archive -> "archive"
    is Screen.Settings -> "settings"
    is Screen.BoardDetail -> "board:$boardId"
    is Screen.ItemDetail -> "item:$itemId"
    is Screen.StatusEditor -> "statuses:$boardId"
    is Screen.BoardSettings -> "boardSettings:$boardId"
    is Screen.SavedView -> "view:$viewId"
    // Deliberately not restored — see the note on NavigatorSaver.
    is Screen.ImportPreview -> "boards"
}

private fun decodeScreen(value: String): Screen {
    val type = value.substringBefore(':')
    val arg = value.substringAfter(':', "")
    return when (type) {
        "today" -> Screen.Today
        "inbox" -> Screen.Inbox
        "search" -> Screen.Search
        "tags" -> Screen.TagManager
        "archive" -> Screen.Archive
        "settings" -> Screen.Settings
        "board" -> Screen.BoardDetail(arg)
        "item" -> Screen.ItemDetail(arg)
        "statuses" -> Screen.StatusEditor(arg)
        "boardSettings" -> Screen.BoardSettings(arg)
        "view" -> Screen.SavedView(arg)
        else -> Screen.Boards
    }
}

private fun decodeTab(value: String): Screen.TopLevel = when (value) {
    "today" -> Screen.Today
    "inbox" -> Screen.Inbox
    "search" -> Screen.Search
    else -> Screen.Boards
}

/**
 * Persists the back stack across configuration changes and process death.
 *
 * Without this a rotation silently returns you to the Boards tab from whatever
 * you were looking at, which reads as the app losing your place — because it
 * is. Verified on a device, not assumed.
 *
 * Encoded as a flat list of strings: the tab history, then each tab's stack
 * preceded by its length. Screens carry at most one string argument, so
 * `type:arg` split on the first colon round-trips ids containing anything.
 *
 * **[Screen.ImportPreview] is not restored** — it degrades to the Boards root.
 * Its argument is a whole JSON payload, and resurrecting a half-confirmed
 * import after process death is worse than making the user pick the file again.
 */
private val NavigatorSaver: Saver<Navigator, Any> = listSaver<Navigator, String>(
    save = { navigator ->
        val (stacks, tabHistory) = navigator.snapshot()
        buildList {
            add(tabHistory.joinToString(",") { it.encode() })
            Navigator.TOP_LEVEL.forEach { tab ->
                val stack = stacks[tab].orEmpty()
                add(stack.size.toString())
                stack.forEach { add(it.encode()) }
            }
        }
    },
    restore = { saved ->
        val tabHistory = saved.first()
            .split(",")
            .filter { it.isNotBlank() }
            .map(::decodeTab)

        val stacks = mutableMapOf<Screen.TopLevel, List<Screen>>()
        var cursor = 1
        Navigator.TOP_LEVEL.forEach { tab ->
            if (cursor >= saved.size) return@forEach
            val size = saved[cursor].toIntOrNull() ?: 0
            cursor++
            val screens = mutableListOf<Screen>()
            repeat(size) {
                if (cursor < saved.size) {
                    screens.add(decodeScreen(saved[cursor]))
                    cursor++
                }
            }
            stacks[tab] = screens
        }

        Navigator(
            restoredStacks = stacks,
            restoredTabHistory = tabHistory,
        )
    },
)

@Composable
fun rememberNavigator(initial: Screen.TopLevel = Screen.Boards): Navigator =
    rememberSaveable(saver = NavigatorSaver) { Navigator(initial) }
