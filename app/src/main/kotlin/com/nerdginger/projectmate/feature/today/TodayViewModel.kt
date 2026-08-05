package com.nerdginger.projectmate.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nerdginger.projectmate.core.focus.FocusSection
import com.nerdginger.projectmate.core.focus.TodayRules
import com.nerdginger.projectmate.core.model.Board
import com.nerdginger.projectmate.data.repository.BoardRepository
import com.nerdginger.projectmate.data.repository.ItemRepository
import com.nerdginger.projectmate.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayUiState(
    val isLoading: Boolean = true,
    val sections: List<FocusSection> = emptyList(),
    /** For turning a row's `boardId` into a name, accent and monogram. */
    val boardsById: Map<String, Board> = emptyMap(),
    val date: LocalDate = LocalDate.EPOCH,
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
}

/**
 * The Today screen's state.
 *
 * Deliberately thin: every rule about what belongs here lives in
 * [TodayRules] in `:core`, where it is unit-tested without a database. This
 * combines three flows, calls that, and stops.
 */
class TodayViewModel(
    itemRepository: ItemRepository,
    boardRepository: BoardRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(
            itemRepository.observeAllLive(),
            boardRepository.observeStatusesById(),
            boardRepository.observeBoardsById(),
        ) { items, statusesById, boardsById ->
            val now: Instant = clock.instant()
            val zone: ZoneId = clock.zone

            TodayUiState(
                isLoading = false,
                sections = TodayRules.sections(items, statusesById, now, zone),
                boardsById = boardsById,
                date = LocalDate.ofInstant(now, zone),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TodayViewModel(container.itemRepository, container.boardRepository)
            }
        }
    }
}
