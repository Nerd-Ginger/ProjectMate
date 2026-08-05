# Changelog

All notable changes to ProjectMate. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Anything a user would notice gets a line here, in the same commit that ships it.

## [Unreleased]

### Changed
- **The app now looks like its design**: a fixed near-black theme with a hot
  orange accent, replacing the Material You wallpaper colours it had been
  picking up. Monospace metadata, monogram board tiles, hairline section rules
  and a segmented progress bar with a per-category legend

### Fixed
- Rotating the device no longer throws away the back stack — the app stayed
  open on the right screen instead of jumping back to the Boards root
- A board's own name is shown when you open it, rather than the word "Board"
- Kanban columns fill the screen height, so the add-item field sits at the
  bottom of a column instead of the card shrink-wrapping around its contents

### Added
- **Item detail** — open anything from a board or from Today and edit it in
  place: title, notes, priority, status, due date, tags and a checklist with a
  progress bar. Everything saves as you go; there is no save button
- **Today** — one screen answering "what needs me now?" across every board.
  Overdue first, then due today, then anything sitting in a status you've marked
  as focus. Each row shows which board it came from, its status and how late it
  is. Sections with nothing in them don't appear at all
- Feature-request import: reads the JSON contract the website will serve, with
  per-request error reporting so one bad entry never loses the rest of a batch,
  and deduplication so re-importing the same export changes nothing
- Kanban board view: one column per status, add items inline, and advance a
  card to the next column with one tap. Columns over their WIP limit say so
- Create boards from templates — Projects, Life, Feature Requests or Simple —
  previewing each template's statuses before committing to one
- Boards home screen showing every collection with a segmented progress bar
  by status category, so a stalled board is visible at a glance
- Offline database: boards, statuses, items, checklists, tags, links and
  feature-request metadata, with first-run seeding of the system Inbox
- Repository scaffolding: `.gitignore`, `.editorconfig`, and contributor
  conventions in `CLAUDE.md`
- Documentation set: purpose and non-goals, roadmap, architecture, data model,
  the feature-request JSON contract, infrastructure costs, and a decision log
