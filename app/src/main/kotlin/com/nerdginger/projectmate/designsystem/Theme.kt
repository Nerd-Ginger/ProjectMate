package com.nerdginger.projectmate.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The black-and-orange design, transcribed from the comp in
 * `design/ProjectMate.dc.html`.
 *
 * **Deliberately fixed — no Material You, no light variant.** The design is a
 * specific near-black surface with a single hot-orange accent, and
 * wallpaper-derived colour would replace exactly the thing that makes the app
 * recognisable. Board accent colours still vary per board; the chrome does not.
 * See docs/DECISIONS.md D-016.
 */

// ---------------------------------------------------------------- raw palette

private val Canvas = Color(0xFF0B0B0C)
private val Surface = Color(0xFF141419)
private val SurfaceRaised = Color(0xFF1A1A20)
private val SurfaceTrack = Color(0xFF1D1D23)
private val SurfaceHigh = Color(0xFF26262E)

private val Orange = Color(0xFFFF6B1A)
private val OrangeBright = Color(0xFFFF8A4C)

private val TextPrimary = Color(0xFFF2F0EE)
private val TextSecondary = Color(0xFF93908C)
private val TextMuted = Color(0xFF7E7B77)
private val TextFaint = Color(0xFF6E6B67)

private val Red = Color(0xFFE2453C)
private val RedBright = Color(0xFFFF7A70)

/**
 * Tokens the Material colour scheme has no slot for: the status-category
 * colours the progress bar and its legend are built from, the hairline borders,
 * and the monospace metadata styling the comp uses throughout.
 */
@Immutable
data class ProjectMateTokens(
    val canvas: Color = Canvas,
    val cardBorder: Color = Color.White.copy(alpha = 0.07f),
    val chipBorder: Color = Color.White.copy(alpha = 0.10f),
    val divider: Color = Color.White.copy(alpha = 0.07f),
    /** Progress-bar track, behind the segments. */
    val track: Color = SurfaceTrack,
    // Status categories. `active` is the accent on purpose — in-flight work is
    // what the orange is for.
    val backlog: Color = Color(0xFF3A3A42),
    val active: Color = Orange,
    val blocked: Color = Red,
    val blockedText: Color = RedBright,
    val done: Color = Color(0xFF6F6C68),
    /** Board avatar / accent options, in the comp's order. */
    val accents: List<Color> = listOf(
        Color(0xFF4A8FE7),
        Color(0xFF2E9E8F),
        Color(0xFFC9A227),
        Color(0xFF8B7BE8),
        Color(0xFFD9628E),
        Color(0xFF6E8B74),
        Orange,
    ),
    /**
     * Metadata style: counts, timestamps, secondary lines. JetBrains Mono in
     * the comp; [FontFamily.Monospace] resolves to Roboto Mono on Android,
     * which is the same idea without shipping a font binary.
     */
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = TextMuted,
    ),
    /** Uppercase, wide-tracked section label — "PINNED", "PROJECTS". */
    val sectionLabel: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
        color = TextFaint,
    ),
)

val LocalProjectMateTokens = staticCompositionLocalOf { ProjectMateTokens() }

private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = Canvas,
    primaryContainer = Color(0xFF2A1708),
    onPrimaryContainer = Orange,
    secondary = OrangeBright,
    onSecondary = Canvas,
    background = Canvas,
    onBackground = TextPrimary,
    surface = Canvas,
    onSurface = TextPrimary,
    surfaceContainerLowest = Canvas,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceHigh,
    surfaceVariant = SurfaceTrack,
    onSurfaceVariant = TextSecondary,
    outline = TextFaint,
    outlineVariant = SurfaceHigh,
    error = Red,
    onError = Canvas,
    errorContainer = Color(0xFF2E100E),
    onErrorContainer = RedBright,
    scrim = Color.Black,
)

private val Base = Typography()

private val ProjectMateTypography = Base.copy(
    // 26sp / medium / slightly tightened — the comp's screen title.
    headlineSmall = Base.headlineSmall.copy(
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = Base.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyMedium = Base.bodyMedium.copy(fontSize = 13.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun ProjectMateTheme(
    tokens: ProjectMateTokens = ProjectMateTokens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalProjectMateTokens provides tokens) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = ProjectMateTypography,
            content = content,
        )
    }
}
