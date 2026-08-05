package com.nerdginger.projectmate.core.template

/**
 * The colours a board or a status can be, taken from the design comp in
 * `design/ProjectMate.dc.html`.
 *
 * Public because two places need to agree: the templates below seed these, and
 * the status editor's colour picker offers them. A picker that offered colours
 * the templates never use — or vice versa — would make a board's palette drift
 * the moment anyone edited a status.
 *
 * Stored as ARGB ints on `Status.colorArgb` and `Board.accentColor`, so `:core`
 * stays free of any Android colour type.
 */
object Palette {

    // -------------------------------------------------------------- statuses

    /** Not started. The quietest thing on a board. */
    const val GREY: Int = 0xFF5C5C64.toInt()

    /** Queued but not quiet — "Next up". A step brighter than [GREY]. */
    const val STONE: Int = 0xFF84818C.toInt()

    /** In flight. The accent, because in-flight work is what the accent is for. */
    const val ORANGE: Int = 0xFFFF6B1A.toInt()

    /** Waiting on something else. The one alarm colour in the palette. */
    const val RED: Int = 0xFFE2453C.toInt()

    /** Active, but not the main event — "In review". */
    const val AMBER: Int = 0xFFFFA05C.toInt()

    /** Finished. Deliberately dim: done work should recede, not celebrate. */
    const val TAUPE: Int = 0xFF6F6C68.toInt()

    // --------------------------------------------------------- board accents

    const val BLUE: Int = 0xFF4A8FE7.toInt()
    const val TEAL: Int = 0xFF2E9E8F.toInt()
    const val GOLD: Int = 0xFFC9A227.toInt()
    const val VIOLET: Int = 0xFF8B7BE8.toInt()
    const val PINK: Int = 0xFFD9628E.toInt()
    const val MOSS: Int = 0xFF6E8B74.toInt()

    /** Offered by the status colour picker, in the order shown. */
    val statusColors: List<Int> = listOf(
        GREY, STONE, ORANGE, AMBER, RED, TAUPE,
        BLUE, TEAL, VIOLET, PINK, GOLD, MOSS,
    )

    /** Offered when choosing a board's accent, in the order shown. */
    val boardAccents: List<Int> = listOf(
        BLUE, TEAL, VIOLET, PINK, GOLD, MOSS, ORANGE,
    )
}
