package com.nerdginger.projectmate.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
) {
    private val stacks: Map<Screen.TopLevel, SnapshotStateList<Screen>> = TOP_LEVEL.associateWith {
        mutableStateListOf<Screen>(it)
    }

    private val tabHistory: SnapshotStateList<Screen.TopLevel> = mutableStateListOf(initial)

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

    companion object {
        val TOP_LEVEL: List<Screen.TopLevel> =
            listOf(Screen.Boards, Screen.Today, Screen.Inbox, Screen.Search)
    }
}

@Composable
fun rememberNavigator(initial: Screen.TopLevel = Screen.Boards): Navigator =
    remember { Navigator(initial) }
