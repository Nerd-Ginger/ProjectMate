# Changelog

All notable changes to ProjectMate. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Anything a user would notice gets a line here, in the same commit that ships it.

## [Unreleased]

### Added
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
