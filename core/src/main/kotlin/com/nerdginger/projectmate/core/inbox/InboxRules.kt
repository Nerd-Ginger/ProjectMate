package com.nerdginger.projectmate.core.inbox

import com.nerdginger.projectmate.core.model.Item
import com.nerdginger.projectmate.core.model.Origin

/** One headed group on the Inbox screen — everything from a single source. */
data class InboxGroup(
    val origin: Origin,
    val items: List<Item>,
) {
    val count: Int get() = items.size

    /** The heading, phrased the way the comp does. */
    val label: String
        get() = when (origin) {
            Origin.SHARE_INTENT -> "shared"
            Origin.FILE_IMPORT -> "imported"
            Origin.WEB_PORTAL -> "from the portal"
            Origin.LOCAL -> "captured here"
        }

    /** The short badge on each row. */
    val badge: String
        get() = when (origin) {
            Origin.SHARE_INTENT -> "shared"
            Origin.FILE_IMPORT -> "imported"
            Origin.WEB_PORTAL -> "portal"
            Origin.LOCAL -> "local"
        }
}

/**
 * How the Inbox is organised.
 *
 * Capture is separate from triage — anything shared, imported or pulled from the
 * website lands on the system Inbox board without being asked which board it
 * belongs to. This decides how that pile is presented.
 *
 * Grouping is by *where it came from* rather than by date, because the answer to
 * "what is this?" differs by source: a portal request needs its requester and
 * vote count, a shared link needs its URL. See docs/SITEMAP.md.
 */
object InboxRules {

    /**
     * Portal requests first — they are the ones with someone waiting on an
     * answer. Locally captured notes last, since you already know what they are.
     */
    private val ORDER = listOf(
        Origin.WEB_PORTAL,
        Origin.SHARE_INTENT,
        Origin.FILE_IMPORT,
        Origin.LOCAL,
    )

    /**
     * @param items everything on the system Inbox board
     * @return one group per source that has anything in it, in [ORDER]
     */
    fun group(items: List<Item>): List<InboxGroup> {
        val live = items.filter { !it.sync.isDeleted && !it.isArchived }
        return ORDER.mapNotNull { origin ->
            val rows = live.filter { it.sync.origin == origin }
            if (rows.isEmpty()) null else InboxGroup(origin, rows)
        }
    }

    /** How many things are waiting. Drives the badge on the bottom bar. */
    fun count(items: List<Item>): Int =
        items.count { !it.sync.isDeleted && !it.isArchived }
}
