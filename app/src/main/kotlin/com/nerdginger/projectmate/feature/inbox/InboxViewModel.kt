package com.nerdginger.projectmate.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nerdginger.projectmate.core.inbox.InboxGroup
import com.nerdginger.projectmate.core.inbox.InboxRules
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.FeatureRequestMeta
import com.nerdginger.projectmate.data.dao.FeatureRequestMetaDao
import com.nerdginger.projectmate.data.mapper.toDomain
import com.nerdginger.projectmate.data.repository.BoardRepository
import com.nerdginger.projectmate.data.repository.ItemRepository
import com.nerdginger.projectmate.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val isLoading: Boolean = true,
    val groups: List<InboxGroup> = emptyList(),
    /** Boards you can triage onto — everything except the Inbox itself. */
    val destinations: List<Board> = emptyList(),
    /** Portal metadata by item id, for requester and vote count. */
    val requestMeta: Map<String, FeatureRequestMeta> = emptyMap(),
    val isSelecting: Boolean = false,
    val selected: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
    val selectedCount: Int get() = selected.size
}

/**
 * Capture and triage.
 *
 * What belongs here and how it groups is [InboxRules] in `:core`; this holds
 * the selection state, which is pure UI and has no business in the domain.
 */
class InboxViewModel(
    private val itemRepository: ItemRepository,
    boardRepository: BoardRepository,
    private val featureRequestMetaDao: FeatureRequestMetaDao,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<InboxUiState> =
        combine(
            itemRepository.observeInbox(),
            boardRepository.observeBoardsById(),
            selection,
        ) { items, boardsById, sel ->
            InboxUiState(
                isLoading = false,
                groups = InboxRules.group(items),
                // The Inbox is not somewhere you triage *to*.
                destinations = boardsById.values.filterNot { it.isSystem }.sortedBy { it.name },
                requestMeta = metaCache,
                isSelecting = sel.active,
                // Drop anything that has since left the Inbox, so a stale id
                // can't keep the action bar claiming a selection that is gone.
                selected = sel.ids intersect items.map { it.id }.toSet(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUiState(),
        )

    private data class Selection(val active: Boolean = false, val ids: Set<String> = emptySet())

    /**
     * Portal metadata, loaded once per visible batch rather than observed.
     *
     * It only changes when an import runs, and one flow per row would mean a
     * query per feature request on a screen built for skimming.
     */
    private var metaCache: Map<String, FeatureRequestMeta> = emptyMap()

    init {
        viewModelScope.launch {
            itemRepository.observeInbox().collect { items ->
                metaCache = featureRequestMetaDao
                    .getForItems(items.map { it.id })
                    .associate { it.itemId to it.toDomain() }
            }
        }
    }

    fun toggleSelecting() {
        selection.value = if (selection.value.active) Selection() else Selection(active = true)
    }

    fun toggleSelected(itemId: String) {
        val current = selection.value
        val ids = if (itemId in current.ids) current.ids - itemId else current.ids + itemId
        selection.value = current.copy(ids = ids)
    }

    /** Moves an item onto a board's default status — triage in one tap. */
    fun moveTo(itemIds: Set<String>, board: Board) = viewModelScope.launch {
        val destination = itemRepository.defaultStatusFor(board.id) ?: return@launch
        itemIds.forEach { id ->
            val item = uiState.value.groups.flatMap { it.items }.firstOrNull { it.id == id }
            if (item != null) itemRepository.moveTo(item, destination)
        }
        selection.value = Selection()
    }

    /** Archive rather than delete — dismissing is not destroying. */
    fun dismiss(itemIds: Set<String>) = viewModelScope.launch {
        itemIds.forEach { itemRepository.archive(it) }
        selection.value = Selection()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                InboxViewModel(
                    itemRepository = container.itemRepository,
                    boardRepository = container.boardRepository,
                    featureRequestMetaDao = container.featureRequestMetaDao,
                )
            }
        }
    }
}
