package com.nerdginger.projectmate.feature.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.data.repository.BoardRepository
import com.nerdginger.projectmate.data.repository.ItemRepository
import com.nerdginger.projectmate.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One kanban column: a status and the items sitting in it. */
data class Column(
    val status: Status,
    val items: List<Item>,
) {
    val isOverWipLimit: Boolean
        get() = status.wipLimit?.let { items.size > it } == true
}

data class BoardDetailUiState(
    val isLoading: Boolean = true,
    val board: Board? = null,
    val columns: List<Column> = emptyList(),
)

class BoardDetailViewModel(
    private val boardId: String,
    private val boardRepository: BoardRepository,
    private val itemRepository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<BoardDetailUiState> =
        combine(
            boardRepository.observeBoardWithStatuses(boardId),
            itemRepository.observeForBoard(boardId),
        ) { boardWithStatuses, items ->
            if (boardWithStatuses == null) {
                BoardDetailUiState(isLoading = false)
            } else {
                val byStatus = items.groupBy { it.statusId }
                BoardDetailUiState(
                    isLoading = false,
                    board = boardWithStatuses.board,
                    columns = boardWithStatuses.statuses.map { status ->
                        Column(
                            status = status,
                            items = byStatus[status.id].orEmpty().sortedBy { it.sortKey },
                        )
                    },
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BoardDetailUiState(),
        )

    fun addItem(statusId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            itemRepository.create(boardId = boardId, statusId = statusId, title = title)
        }
    }

    fun move(item: Item, destination: Status) {
        viewModelScope.launch { itemRepository.moveTo(item, destination) }
    }

    /**
     * Moves an item to the next column along.
     *
     * The common case by a wide margin, and worth a single tap rather than
     * opening a picker to choose the status that was already sitting to the
     * right of it.
     */
    fun advance(item: Item) {
        val columns = uiState.value.columns
        val index = columns.indexOfFirst { it.status.id == item.statusId }
        val next = columns.getOrNull(index + 1) ?: return
        move(item, next.status)
    }

    fun archive(itemId: String) {
        viewModelScope.launch { itemRepository.archive(itemId) }
    }

    companion object {
        fun factory(container: AppContainer, boardId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BoardDetailViewModel(
                        boardId = boardId,
                        boardRepository = container.boardRepository,
                        itemRepository = container.itemRepository,
                    )
                }
            }
    }
}
