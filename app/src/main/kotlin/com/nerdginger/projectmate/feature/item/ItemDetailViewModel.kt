package com.nerdginger.projectmate.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.ChecklistEntry
import com.nerdginger.projectmate.core.model.FeatureRequestMeta
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.model.Tag
import com.nerdginger.projectmate.data.repository.BoardRepository
import com.nerdginger.projectmate.data.repository.ItemDetailRepository
import com.nerdginger.projectmate.data.repository.ItemRepository
import com.nerdginger.projectmate.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: Item? = null,
    val board: Board? = null,
    val status: Status? = null,
    /** Every status on this item's board, for the status picker. */
    val boardStatuses: List<Status> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val checklist: List<ChecklistEntry> = emptyList(),
    val featureRequest: FeatureRequestMeta? = null,
) {
    /** True once loading finished and the item genuinely isn't there. */
    val isMissing: Boolean get() = !isLoading && item == null

    val checklistDone: Int get() = checklist.count { it.isDone }

    /** `0f` when there is nothing to complete, so the bar renders empty. */
    val checklistFraction: Float
        get() = if (checklist.isEmpty()) 0f else checklistDone.toFloat() / checklist.size
}

// The comp's "Linked items" section is deliberately absent. ItemLinkDao and the
// repository can already read and remove links, but nothing in the app can
// create one yet, so the section could never appear. It goes in with whatever
// UI creates links, rather than shipping as a block that is always empty.

/**
 * Item detail.
 *
 * Autosaves — every edit writes straight through. There is no save button and
 * no draft state to lose, which is why the top bar can just say "SAVED".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailViewModel(
    private val itemId: String,
    private val itemRepository: ItemRepository,
    private val itemDetailRepository: ItemDetailRepository,
    boardRepository: BoardRepository,
) : ViewModel() {

    private val itemFlow: Flow<Item?> = itemRepository.observeById(itemId)

    /**
     * The board's own statuses, followed only once the item is known.
     *
     * `flatMapLatest` rather than a fixed flow because the item can move to a
     * different board, and the status picker must follow it there.
     */
    private val boardContext: Flow<Triple<Board?, Status?, List<Status>>> =
        itemFlow.flatMapLatest { item ->
            if (item == null) {
                flowOf(Triple(null, null, emptyList()))
            } else {
                combine(
                    boardRepository.observeBoardWithStatuses(item.boardId),
                    boardRepository.observeStatusesById(),
                ) { boardWithStatuses, statusesById ->
                    Triple(
                        boardWithStatuses?.board,
                        statusesById[item.statusId],
                        boardWithStatuses?.statuses.orEmpty(),
                    )
                }
            }
        }

    private val details: Flow<Details> = combine(
        itemDetailRepository.observeChecklist(itemId),
        itemDetailRepository.observeTags(itemId),
        itemDetailRepository.observeAllTags(),
        itemDetailRepository.observeFeatureRequest(itemId),
    ) { checklist, tags, allTags, featureRequest ->
        Details(checklist, tags, allTags, featureRequest)
    }

    val uiState: StateFlow<ItemDetailUiState> =
        combine(itemFlow, boardContext, details) { item, context, detail ->
            val (board, status, boardStatuses) = context
            ItemDetailUiState(
                isLoading = false,
                item = item,
                board = board,
                status = status,
                boardStatuses = boardStatuses,
                tags = detail.tags,
                allTags = detail.allTags,
                checklist = detail.checklist,
                featureRequest = detail.featureRequest,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemDetailUiState(),
        )

    private data class Details(
        val checklist: List<ChecklistEntry>,
        val tags: List<Tag>,
        val allTags: List<Tag>,
        val featureRequest: FeatureRequestMeta?,
    )

    // ----------------------------------------------------------------- edits

    private fun edit(block: suspend (Item) -> Unit) {
        val item = uiState.value.item ?: return
        viewModelScope.launch { block(item) }
    }

    fun setTitle(title: String) = edit { item ->
        val trimmed = title.trim()
        if (trimmed.isNotEmpty() && trimmed != item.title) {
            itemRepository.update(item.copy(title = trimmed))
        }
    }

    fun setNotes(notes: String) = edit { item ->
        val cleaned = notes.ifBlank { null }
        if (cleaned != item.notes) itemRepository.update(item.copy(notes = cleaned))
    }

    fun setPriority(priority: Priority) = edit { item ->
        if (priority != item.priority) itemRepository.update(item.copy(priority = priority))
    }

    /** `null` clears the due date entirely. */
    fun setDue(dueAt: Long?, hasTime: Boolean) = edit { item ->
        itemRepository.update(item.copy(dueAt = dueAt, dueHasTime = dueAt != null && hasTime))
    }

    fun moveTo(status: Status) = edit { item ->
        itemRepository.moveTo(item, status)
    }

    fun archive() = edit { item -> itemRepository.archive(item.id) }

    fun addChecklistEntry(text: String) = viewModelScope.launch {
        itemDetailRepository.addChecklistEntry(itemId, text)
    }

    fun setChecklistDone(entry: ChecklistEntry, done: Boolean) = viewModelScope.launch {
        itemDetailRepository.setChecklistDone(entry.id, done)
    }

    fun deleteChecklistEntry(entry: ChecklistEntry) = viewModelScope.launch {
        itemDetailRepository.deleteChecklistEntry(entry.id)
    }

    fun addTag(name: String) = viewModelScope.launch {
        itemDetailRepository.addTag(itemId, name)
    }

    fun removeTag(tag: Tag) = viewModelScope.launch {
        itemDetailRepository.removeTag(itemId, tag.id)
    }

    companion object {
        fun factory(container: AppContainer, itemId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ItemDetailViewModel(
                        itemId = itemId,
                        itemRepository = container.itemRepository,
                        itemDetailRepository = container.itemDetailRepository,
                        boardRepository = container.boardRepository,
                    )
                }
            }
    }
}
