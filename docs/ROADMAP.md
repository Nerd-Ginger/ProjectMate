# Roadmap

Status legend: ✅ shipped · 🚧 in progress · ⬜ planned · 💭 idea, not committed

Last updated: 2026-08-03

---

## Phase 1 — The app (offline, no backend)

The goal: an app that is genuinely usable every day with no server involved.

### Foundation

- ✅ Repository scaffolding, conventions, `.gitignore`
- ✅ Documentation set (purpose, architecture, data model, infrastructure)
- ✅ Gradle build — version catalog, `:core` (pure JVM) + `:app` (Android)
- ⬜ GitHub Actions CI producing a downloadable debug APK

### Core logic (`:core`, locally testable)

- ✅ Domain models — collections, statuses, items, tags, checklists
- ✅ Status categories (`Backlog`/`Active`/`Blocked`/`Done`/`Cancelled`) and the
  focus flag
- ✅ Board templates — Projects, Life, Feature Requests, Simple
- ✅ Today rules — what counts as needing attention today
- ✅ Backup JSON schema (full export/import)
- ✅ Feature-request JSON schema and importer, with dedup

### Data (`:app`)

- ✅ Room entities, DAOs, database (no type converters needed)
- ✅ Exported schemas committed under `app/schemas/` (by CI)
- ✅ First-run seeding — Inbox system collection
- ✅ Repositories and `AppContainer` DI

### Interface

- ✅ Design-system theme — the black-and-orange comp, navigation shell
- ✅ **Boards** — collection cards with progress
- ✅ Create collection from template
- ✅ **Board view** — kanban columns, move and advance status
- ⬜ **List view** — grouped by status or due date
- ✅ **Item detail** — notes, priority, due date, tags, checklist
- ✅ **Today** — cross-board attention view
- ✅ **Inbox** — capture and triage
- ✅ **Search** across all items
- ⬜ **Status editor** — add, rename, recolour, reorder, set category
- ⬜ **Settings** — export/import JSON, tags, theme, about

### Bridges (work without a server)

- ⬜ Share target — share text or a URL into the app, lands in Inbox
- ⬜ File import — pick a feature-request JSON file
- ⬜ Deep link — `projectmate://import?…`
- ⬜ Full backup export/import

**Phase 1 is done when:** a Projects board and a Life board can be created,
items move through statuses, Today shows the right things, a shared URL lands in
the Inbox, and a full export re-imports cleanly.

---

## Phase 2 — The feature-request portal

Runs on Cloudflare. Costs $0–5/month. See
[`INFRASTRUCTURE.md`](INFRASTRUCTURE.md) for the full breakdown and why no VPS
is needed.

- ⬜ Cloudflare Pages site (replacing Squarespace)
- ⬜ Public request form, protected by Turnstile
- ⬜ Worker API — `POST /api/requests`, `GET /api/requests?since=`
- ⬜ D1 schema for stored requests
- ⬜ Admin token so only the app can read the queue
- ⬜ In-app pull sync via WorkManager, reusing the phase-1 importer unchanged
- ⬜ Dedup on repeat pulls
- ⬜ Optional: public status page showing what's shipped

**Deliberate design choice:** the Worker returns *exactly* the JSON schema the
phase-1 file importer already understands. No new parsing code, and the file
import path stays as a manual fallback forever.

---

## Phase 3 — Multi-device sync

Only worth building if a second device is actually in use. The data model is
already prepared for it (UUID keys, `updatedAt`, tombstones) so this is additive.

- ⬜ Sync endpoint (Worker + D1)
- ⬜ Push/pull with last-write-wins per record
- ⬜ Conflict surfacing where last-write-wins would silently lose an edit
- ⬜ Encrypted backup to R2

---

## Later — not committed

- 💭 Home-screen widget for Today
- 💭 Reminders and notifications for due items
- 💭 Recurring items
- 💭 Item links (this request implements that project)
- 💭 Attachments and images
- 💭 Quick-add tile / assistant shortcut
- 💭 CSV export
- 💭 Per-board archive view and bulk actions
- 💭 Wear OS companion

## Explicitly rejected

These are non-goals — see [`PURPOSE.md`](PURPOSE.md#non-goals) for the reasoning.
Time tracking · calendar replacement · team accounts and assignees · automation
rules · anything that requires a server to function.

---

## Keeping this current

Tick items off in the commit that ships them. When a phase completes, note the
date. If something moves from 💭 to ⬜, it means it's been decided on — record
the reasoning in [`DECISIONS.md`](DECISIONS.md).
