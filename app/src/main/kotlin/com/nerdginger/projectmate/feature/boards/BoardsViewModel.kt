package com.nerdginger.projectmate.feature.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nerdginger.projectmate.core.template.BoardTemplate
import com.nerdginger.projectmate.core.template.BoardTemplates
import com.nerdginger.projectmate.data.repository.BoardRepository
import com.nerdginger.projectmate.data.repository.BoardSummary
import com.nerdginger.projectmate.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BoardsUiState(
    val isLoading: Boolean = true,
    val pinned: List<BoardSummary> = emptyList(),
    val others: List<BoardSummary> = emptyList(),
    val templates: List<BoardTemplate> = BoardTemplates.all,
    val errorMessage: String? = null,
) {
    /** True once loading has finished and there is genuinely nothing to show. */
    val isEmpty: Boolean get() = !isLoading && pinned.isEmpty() && others.isEmpty()
}

class BoardsViewModel(
    private val repository: BoardRepository,
) : ViewModel() {

    private val errors = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BoardsUiState> =
        combine(
            repository.observeBoardSummaries()
                .catch { throwable ->
                    errors.value = throwable.message ?: "Could not load boards."
                    emit(emptyList())
                },
            errors.asStateFlow(),
        ) { summaries, error ->
            BoardsUiState(
                isLoading = false,
                // The Inbox is a system board and belongs with the pinned set
                // rather than mixed in with boards the user made.
                pinned = summaries.filter { it.board.isPinned },
                others = summaries.filterNot { it.board.isPinned },
                errorMessage = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BoardsUiState(),
        )

    init {
        viewModelScope.launch {
            runCatching { repository.seedIfEmpty() }
                .onFailure { errors.value = it.message ?: "Could not prepare the database." }
        }
    }

    fun createBoard(template: BoardTemplate, name: String) {
        viewModelScope.launch {
            runCatching { repository.createFromTemplate(template, name.ifBlank { template.name }) }
                .onFailure { errors.value = it.message ?: "Could not create the board." }
        }
    }

    fun setPinned(boardId: String, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(boardId, pinned) }
    }

    fun archive(boardId: String) {
        viewModelScope.launch { repository.archive(boardId) }
    }

    fun dismissError() {
        errors.value = null
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { BoardsViewModel(container.boardRepository) }
        }
    }
}
