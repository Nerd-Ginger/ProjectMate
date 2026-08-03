package com.nerdginger.projectmate.core.focus

import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Priority
import com.nerdginger.projectmate.core.model.Status
import com.nerdginger.projectmate.core.time.DueBucket
import com.nerdginger.projectmate.core.time.DueDates
import java.time.Instant
import java.time.ZoneId

/** Why an item earned a place on the Today screen. */
enum class FocusReason {
    OVERDUE,
    DUE_TODAY,
    /** Sitting in a status the user marked as focus — "what I'm actually on". */
    IN_FOCUS_STATUS,
    ;
}

/** One row of the Today screen. */
data class FocusEntry(
    val item: Item,
    val status: Status,
    val reason: FocusReason,
    val bucket: DueBucket,
)

/**
 * Decides what belongs on the Today screen.
 *
 * Today is the screen the whole app is judged on: it is the one honest answer
 * to "what needs my attention now", pulled across every board. If it is noisy
 * it stops being trusted, and an untrusted Today screen is worse than no Today
 * screen — so the rules here are deliberately narrow, and live in `:core` where
 * they can be tested properly.
 *
 * See docs/PURPOSE.md, principle 5.
 */
object TodayRules {

    /**
     * @param items every non-deleted item, from every board
     * @param statusesById every status, keyed by id
     */
    fun today(
        items: List<Item>,
        statusesById: Map<String, Status>,
        now: Instant,
        zone: ZoneId,
    ): List<FocusEntry> =
        items.asSequence()
            .filter { !it.sync.isDeleted && !it.isArchived }
            .mapNotNull { item ->
                val status = statusesById[item.statusId] ?: return@mapNotNull null

                // Finished and abandoned work is never pressing, whatever its
                // due date says. A completed item with a past due date is not a
                // problem to be surfaced.
                if (status.category.isTerminal) return@mapNotNull null

                val bucket = DueDates.bucket(item.dueAt, item.dueHasTime, now, zone)
                val reason = when {
                    bucket == DueBucket.OVERDUE -> FocusReason.OVERDUE
                    bucket == DueBucket.TODAY -> FocusReason.DUE_TODAY
                    status.isFocus -> FocusReason.IN_FOCUS_STATUS
                    else -> return@mapNotNull null
                }

                FocusEntry(item, status, reason, bucket)
            }
            .sortedWith(ordering)
            .toList()

    /**
     * Overdue first, then due today, then focus work. Within a group, more
     * urgent first, then the earlier due date, then title for a stable order.
     */
    private val ordering: Comparator<FocusEntry> =
        compareBy<FocusEntry> { it.reason.ordinal }
            .thenByDescending { it.item.priority.weight }
            .thenBy { it.item.dueAt ?: Long.MAX_VALUE }
            .thenBy { it.item.title.lowercase() }

    private val Priority.weight: Int get() = id
}
