# Site map

Every view in ProjectMate, what it's for, and what it contains.

**Status: mostly specification.** The navigation shell, the Boards home, the
kanban board and Today exist and have been run on a device; every other view is
a named placeholder in the app, so a build on a phone shows exactly how far
things have got.

This document is the design target — it defines what gets built. The **visual**
reference is the comp at `design/ProjectMate.dc.html`, which renders every
screen listed here; this file says what a screen contains, the comp says what it
looks like. Views are marked ⬜ planned / 🚧 building / ✅ shipped and updated as
they land.

Last updated: 2026-08-04

---

## Shape of the app

Single Activity. A back stack you push onto, four persistent top-level
destinations, and a set of sheets that never take you off the current screen.

```
Scaffold + NavigationBar        bottom bar (a tablet rail is a later change)
├── Boards          (top level)  every collection you own
├── Today           (top level)  what needs attention, across all boards
├── Inbox           (top level)  captured and imported, awaiting triage
└── Search          (top level)  everything, filtered

pushed onto the back stack from any of the above:
    Board → Item detail → …
    Status editor · Board settings · Saved view · Tag manager
    Import preview · Archive · Settings · About
```

**Two nav rules worth designing around:**
1. The four top-level destinations keep independent back stacks. Switching tabs
   never loses your place.
2. Anything that would lose context is a bottom sheet, not a screen — quick add,
   status picker, date picker, tag picker, board picker.

---

## Top-level destinations

### 1. Boards — home ✅

The collections list. Answers "what am I tracking?"

| Contains | Notes |
|---|---|
| Smart-view strip | Pinned dynamic collections: Today, Inbox, Overdue. Horizontally scrollable chips with counts |
| Pinned boards | Cards, above the rest |
| Board cards | Emoji, name, accent colour, **segmented progress bar by status category**, open-item count, next due date |
| Grouped sections | By board type — Projects, Life, other |
| FAB | New board → template picker |
| Overflow | Archive, tag manager, settings |

**Empty state:** first run offers to seed Projects and Life boards.
**Design note:** the progress bar segments are `Backlog / Active / Blocked /
Done` — four colours, not a percentage. Blocked needs to be visible at a glance.

### 2. Today ✅

The screen the app is judged on. One honest answer to "what now?", pulled
across every board.

| Section | Rule |
|---|---|
| **Overdue** | Past due. All-day items only after their day has ended |
| **Due today** | Due today, not yet passed |
| **In focus** | Sitting in a status marked as focus — work with no due date |

Rows carry their board's monogram in its accent colour, so a work project and a
household errand are distinguishable at a glance. Terminal statuses never
appear, however overdue.

**Built as:** a monogram badge, the title, a status pill and a due chip, with an
orange dot for urgent items only. Section headings carry their own count and
disappear entirely when empty — an empty "Overdue" heading would imply something
is wrong when nothing is. Every rule lives in `TodayRules` in `:core`; the screen
only draws the answer.

**Empty state:** matters more than usual — "nothing needs you today" should feel
earned, not broken.
**Design note:** deliberately narrow. Anything else here erodes trust in it.

### 3. Inbox ⬜

Capture is separate from triage. Anything shared, imported, or pulled from the
website lands here without demanding a board or status first.

| Contains | Notes |
|---|---|
| Untriaged items | Grouped by source: shared, imported, from the portal |
| Source badge | Where each came from |
| Per-item actions | Move to board, set status, tag, dismiss |
| Bulk mode | Multi-select for the same actions |

**Design note:** feature requests from the website arrive here. A row needs to
show enough — requester, votes, first line of the body — to triage without
opening it.

### 4. Search ⬜

| Contains | Notes |
|---|---|
| Query field | Title and notes |
| Filter chips | Board, status category, tag, priority, due window |
| Results | Grouped by board |
| Save action | "Save as view" → becomes a saved view |

---

## Board views

### 5. Board — Kanban ✅

| Contains | Notes |
|---|---|
| Status columns | Horizontally scrolled, in the board's own order |
| Column header | Name, colour, count, WIP limit |
| WIP warning | Column tints when over its limit — a nudge, nothing is blocked |
| Item cards | Title, priority dot, due badge, tag dots, checklist progress |
| Advance button | One tap moves a card to the next column — the common case. Drag-and-drop is a later change; sort keys already support it |
| Top bar | Board name, view-mode toggle, group-by, overflow |

**Design note:** cards must stay readable at column width on a phone. Notes are
never loaded into a card.

### 6. Board — List ⬜

Same data, denser. Grouped by the board's group-by setting (status, due date,
priority, or tag). Swipe to complete or archive.

### 7. Board — Agenda ⬜

Date-bucketed: Overdue · Today · Tomorrow · This week · Later · No date. The
right view for a Life board.

### 8. Item detail ⬜

| Contains | Notes |
|---|---|
| Title | Inline editable |
| Notes | Markdown |
| Status chip | → status picker sheet |
| Priority | None / Low / Normal / Urgent |
| Due date | **With an all-day toggle** — this distinction is load-bearing |
| Tags | → tag picker sheet |
| Checklist | Reorderable, with a progress bar |
| Linked items | Relates / Blocks / Duplicate of / Implements |
| Source badge | Only when not created by hand |
| **Feature-request card** | Only for feature requests: requester, votes, submitted date, portal link, "convert to project task" |

**Autosave on every field change. No Save button, no destructive back.**

---

## Configuration

### 9. Status editor ⬜

Per board. The screen that makes per-board statuses real.

| Contains | Notes |
|---|---|
| Status rows | Drag to reorder |
| Per status | Name, colour, **category dropdown**, default toggle, focus toggle, WIP limit |
| Add status | |
| Delete | **Forces reassigning that status's items** — the FK is RESTRICT on purpose |

**Design note:** the category dropdown needs explaining in the UI. It's the
mechanism that lets everything else keep working when you rename a column.

### 10. Board settings ⬜

Name, emoji, accent colour, default view mode, default group-by, default item
type, **portal slug** (which website project routes here), landing status for
imports, archive, delete.

### 11. Tag manager ⬜

Rename, recolour, merge, delete — each with a usage count.

### 12. Saved view ⬜

Renders any stored filter. Today, Inbox and Overdue are built-in instances of
this — one engine, no special cases. User-created views come from Search.

### 13. Archive ⬜

Archived boards and items. Restore or purge.

### 14. Settings ⬜

| Group | Contains |
|---|---|
| Appearance | Theme (system/light/dark), dynamic colour, start screen |
| Data | Export backup, import backup, purge old tombstones, storage stats |
| Portal | Slug → board mappings (phase 2: API token, sync interval) |
| About | Version, docs links, licences |

### 15. Onboarding ⬜

One screen, skippable. Pick which starter boards to seed.

---

## Import

### 16. Import preview ⬜

**Nothing ever imports silently.** Every transport lands here first.

| Contains | Notes |
|---|---|
| Parse summary | *N new · M updates · K rejected* |
| Rejected rows | With a reason each — one bad row never kills a batch |
| Target picker | Board and landing status |
| Remember mapping | Offers to store the slug → board mapping |
| Confirm | Commits the whole batch in one transaction |

Reached from: share intent, file open, `projectmate://` deep link, and later
the Worker pull. See [`FEATURE_REQUEST_SCHEMA.md`](FEATURE_REQUEST_SCHEMA.md).

---

## Sheets and dialogs

Not screens — they preserve the context behind them.

| Sheet | Notes |
|---|---|
| **Quick add** | Global FAB. One text field with light parsing: `!!` urgent, `#tag`, `@board`, `tomorrow`/`fri` |
| **New board** ✅ | Template picker, showing each template's statuses and their categories before you commit |
| Status picker | Grouped by category |
| Date picker | **All-day toggle included** |
| Tag picker | With inline create |
| Board picker | For moving items |
| Colour picker | Shared by boards and statuses |
| Confirm delete | Only where the action isn't reversible |

---

## What you need to design

Counting real design surfaces: **16 screens, 7 sheets.**

If you're prioritising, these four carry the app:

1. **Boards home** — first impression, and the progress bar is the hardest bit
2. **Board kanban** — where the time is spent
3. **Item detail** — the most fields, the most chance to feel cluttered
4. **Today** — the reason to open the app daily

These need the most thought per pixel:

- **Status editor**, because the category concept has to be legible
- **Import preview**, because it has to make a batch operation feel safe

## Components to define once

Status chip · priority dot · due badge · board card · item card · segmented
progress bar · tag dot row · checklist row · empty states · colour swatch row ·
reorderable list handle · source badge.

---

## Keeping this current

Flip a view's marker in the commit that ships it. Adding a screen means adding
it here in the same commit — the rule in `CLAUDE.md`.
