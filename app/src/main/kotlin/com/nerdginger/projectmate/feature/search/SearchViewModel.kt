package com.nerdginger.projectmate.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.StatusCategory
import com.nerdginger.projectmate.core.time.DueBucket
import com.nerdginger.projectmate.core.time.DueDates
import com.nerdginger.projectmate.data.repository.BoardRepository
import com.nerdginger.projectmate.data.repository.ItemRepository
import com.nerdginger.projectmate.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

/** One thing you can narrow a search by. All active filters must match. */
enum class SearchFilter(val label: String) {
    ACTIVE("Active"),
    BLOCKED("Blocked"),
    URGENT("Urgent"),
    DUE_THIS_WEEK("Due this week"),
}

/** Results for one board. */
data class SearchGroup(
    val board: Board?,
    val items: List<Item>,
    val statusesById: Map<String, Status>,
) {
    val count: Int get() = items.size
}

data class SearchUiState(
    val query: String = "",
    val filters: Set<SearchFilter> = emptySet(),
    val groups: List<SearchGroup> = emptyList(),
    val totalCount: Int = 0,
    val isSearching: Boolean = false,
) {
    /** "21 items" or "4 items  ·  2 filters", as the comp phrases it. */
    val summary: String
        get() = buildString {
            append("$totalCount ${if (totalCount == 1) "item" else "items"}")
            if (filters.isNotEmpty()) {
                append("  ·  ${filters.size} ${if (filters.size == 1) "filter" else "filters"}")
            }
        }

    /** Nothing typed and nothing ticked — the resting state, not "no results". */
    val isIdle: Boolean get() = query.isBlank() && filters.isEmpty()

    val isEmpty: Boolean get() = !isIdle && groups.isEmpty()
}

/**
 * Cross-board search.
 *
 * Matching on text is SQL's job (`ItemDao.search`, a LIKE over title and notes);
 * the filters are applied in Kotlin afterwards, because they read a status's
 * category rather than any column on the item — the same indirection that lets
 * statuses be renamed freely (D-002).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val itemRepository: ItemRepository,
    boardRepository: BoardRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(emptySet<SearchFilter>())

    /**
     * Debounced so a query runs per pause, not per keystroke.
     *
     * `flatMapLatest` cancels the previous query when a newer one arrives, so
     * a slow result can never overwrite a fresher one.
     */
    private val matches = query
        .debounce(200)
        .flatMapLatest { text -> itemRepository.search(text.trim()) }

    val uiState: StateFlow<SearchUiState> =
        combine(
            query,
            filters,
            matches,
            boardRepository.observeStatusesById(),
            boardRepository.observeBoardsById(),
        ) { text, active, items, statusesById, boardsById ->
            if (text.isBlank() && active.isEmpty()) {
                return@combine SearchUiState()
            }

            val kept = items.filter { item ->
                active.all { it.matches(item, statusesById[item.statusId]) }
            }

            SearchUiState(
                query = text,
                filters = active,
                totalCount = kept.size,
                // Board order, so results sit where you expect rather than
                // shuffling as you type.
                groups = kept.groupBy { it.boardId }
                    .map { (boardId, rows) ->
                        SearchGroup(boardsById[boardId], rows, statusesById)
                    }
                    .sortedBy { it.board?.sortKey ?: "" },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState(),
        )

    private fun SearchFilter.matches(item: Item, status: Status?): Boolean = when (this) {
        SearchFilter.ACTIVE -> status?.category == StatusCategory.ACTIVE
        SearchFilter.BLOCKED -> status?.category == StatusCategory.BLOCKED
        SearchFilter.URGENT -> item.priority == Priority.URGENT
        SearchFilter.DUE_THIS_WEEK -> {
            val bucket = DueDates.bucket(item.dueAt, item.dueHasTime, clock.instant(), clock.zone)
            bucket in setOf(
                DueBucket.OVERDUE,
                DueBucket.TODAY,
                DueBucket.TOMORROW,
                DueBucket.THIS_WEEK,
            )
        }
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun toggleFilter(filter: SearchFilter) {
        filters.value = if (filter in filters.value) filters.value - filter else filters.value + filter
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(container.itemRepository, container.boardRepository)
            }
        }
    }
}
