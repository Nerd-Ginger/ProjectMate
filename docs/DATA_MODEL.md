# Data model

Room schema **v1**. Exported JSON lives in `app/schemas/` and is committed —
changing an entity without bumping the version and writing a migration fails the
build.

Last updated: 2026-08-03

---

## Shared conventions

### `SyncMeta` — embedded in every synced entity

Present in schema v1 even though phase 1 has no server, because adding it later
means backfilling identifiers for rows that already exist on exactly one device.
That's the classic offline-first trap; the columns cost a few bytes now.

| Field | Type | Purpose |
|---|---|---|
| `createdAt` | `Long` | epoch millis, UTC |
| `updatedAt` | `Long` | last-write-wins comparison key; also the delta cursor |
| `deletedAt` | `Long?` | **tombstone**. Non-null means deleted. Never `DELETE`. |
| `remoteId` | `String?` | set when a server minted its own key, so re-import reconciles instead of duplicating |
| `syncState` | `Int` | `0` local-only · `1` pending push · `2` synced · `3` conflict |
| `version` | `Long` | server-incremented revision. `updatedAt` alone loses edits under clock skew; this makes a conflict *detectable* rather than silently resolved |
| `origin` | `Int` | `0` local · `1` share intent · `2` file import · `3` web portal |

Primary keys sit on the entity itself, not in `SyncMeta`.

### Identifiers

`String` primary keys holding **UUIDv7**, generated on-device. Time-ordered, so
they cluster well in a B-tree and carry an implicit creation order. Never
autoincrement integers — they collide across devices and would force a migration
that rewrites every foreign key.

### Ordering — `sortKey: String`

Fractional string indices, LexoRank-style: `"a0"`, `"a0h"`, `"a1"`. Inserting
between two neighbours mints a key strictly between them, so a drag is **one
row UPDATE**. Two devices reordering at once merge without either losing.

A `Double` position was the obvious alternative and is what most tutorials use.
It breaks: repeatedly inserting between the same two cards halves the gap each
time and exhausts float precision in about fifty moves. Strings don't have a
floor. Implemented and tested in `:core` (`sort/SortKey.kt`).

### Soft deletes

Every read filters `deletedAt IS NULL`. Tombstones older than 90 days can be
purged from Settings.

---

## Entities

### `boards` — a collection

| Field | Type | Notes |
|---|---|---|
| `id` | `String` PK | UUIDv7 |
| `name` | `String` | |
| `description` | `String?` | |
| `boardType` | `Int` | `PROJECTS` · `LIFE` · `FEATURE_REQUESTS` · `CUSTOM` — a hint for defaults only, never a behaviour switch |
| `templateId` | `String?` | which preset seeded it |
| `emoji` | `String?` | |
| `accentColor` | `Int` | ARGB |
| `defaultViewMode` | `Int` | `KANBAN` · `LIST` · `AGENDA` |
| `defaultGroupBy` | `Int` | `STATUS` · `DUE` · `PRIORITY` · `TAG` |
| `portalSlug` | `String?` | unique. Maps an incoming feature request's `projectSlug` to this board |
| `portalDefaultStatusId` | `String?` | where imported requests land |
| `sortKey` | `String` | |
| `isPinned` | `Boolean` | |
| `isSystem` | `Boolean` | true for Inbox; can't be deleted |
| `archivedAt` | `Long?` | |
| + `SyncMeta` | | |

### `statuses` — per board, ordered, coloured

| Field | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `boardId` | `String` FK → `boards`, **CASCADE**, indexed | |
| `name` | `String` | fully user-editable |
| `category` | `Int` | **the keystone — see below** |
| `colorArgb` | `Int` | |
| `sortKey` | `String` | column order |
| `isDefault` | `Boolean` | where new items land |
| `isFocus` | `Boolean` | surfaces in Today regardless of due date |
| `wipLimit` | `Int?` | soft warning only |
| + `SyncMeta` | | |

**`StatusCategory`** — `INBOX`, `BACKLOG`, `ACTIVE`, `BLOCKED`, `DONE`,
`CANCELLED`.

This is what makes unlimited user-defined statuses safe. Boards invent their own
vocabulary — "Scoping", "Waiting on parts", "Shipped" — but each maps to one
category, so progress percentages, "hide completed", "what's blocked
everywhere", and the Today view work generically without pattern-matching on
names. Rename "Shipped" to "Launched" and nothing downstream notices.

**Seed templates** live in `:core` as code, not rows:

| Template | Statuses |
|---|---|
| Projects | Idea · Planning · Building · Blocked · In Review · Shipped · Shelved |
| Life | Inbox · Today · This Week · Waiting On · Someday · Done |
| Feature Requests | Triage · Accepted · In Progress · Shipped · Declined · Duplicate |
| Simple | To Do · Doing · Done |

### `items` — one table for projects, tasks and feature requests

| Field | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `boardId` | `String` FK → `boards`, CASCADE, indexed | |
| `statusId` | `String` FK → `statuses`, **RESTRICT**, indexed | restrict is deliberate: deleting a status forces reassigning its items rather than orphaning them |
| `title` | `String` | |
| `notes` | `String?` | markdown |
| `itemType` | `Int` | `PROJECT` · `TASK` · `FEATURE_REQUEST` · `NOTE` |
| `priority` | `Int` | 0 none · 1 low · 2 normal · 3 urgent |
| `dueAt` | `Long?` | |
| `dueHasTime` | `Boolean` | **all-day vs timed.** Without it a "due Tuesday" item is stored as Tuesday 00:00 and reads as overdue for the whole day |
| `startAt` | `Long?` | |
| `remindAt` | `Long?` | reserved; notifications are a later phase |
| `completedAt` | `Long?` | |
| `parentItemId` | `String?` | sub-projects |
| `externalRequestId` | `String?` | **unique index** — the import dedup key |
| `sortKey` | `String` | |
| `isPinned` | `Boolean` | |
| `archivedAt` | `Long?` | |
| + `SyncMeta` | | |

Indices: `(boardId, statusId, sortKey)`, `(dueAt)`, `(archivedAt)`, unique
`(externalRequestId)`.

### `checklist_entries`

`id` PK · `itemId` FK CASCADE indexed · `text` · `isDone` · `doneAt: Long?` ·
`sortKey` · `SyncMeta`

### `tags`

`id` PK · `name` (unique, case-folded) · `colorArgb` · `SyncMeta`

Global rather than per-board, so tags are the axis that cuts across collections.

### `item_tags`

Composite PK `(itemId, tagId)` · `createdAt` · `deletedAt: Long?` · `syncState`

Join rows carry tombstones too. Without them, removing a tag on one device gets
undone by another device's stale copy on the next sync.

### `item_links`

`id` PK · `fromItemId` · `toItemId` · `relation: Int`
(`BLOCKS` · `RELATES` · `DUPLICATE_OF` · `IMPLEMENTS`) · `SyncMeta`

This is how a feature request on the Feature Requests board connects to the
actual work item on a Projects board.

### `feature_request_meta` — 1:1 extension

`itemId` PK / FK CASCADE · `requesterName: String?` · `requesterEmail: String?`
· `contactOptIn: Boolean` · `votes: Int` · `portalSlug: String?` ·
`submittedAt: Long` · `rawPayloadJson: String` · `SyncMeta`

Keeps portal fields and personal data out of the hot `items` table.
`rawPayloadJson` stores the original request verbatim — you can't lose fields
you haven't modelled yet.

### `saved_views` — dynamic collections

`id` PK · `name` · `emoji` · `filterJson: String` · `sortKey` · `isPinned` ·
`isBuiltIn: Boolean` · `SyncMeta`

Boards are *static* collections: an item lives in exactly one. Saved views are
*dynamic* ones: a stored query. **Today, Inbox and Overdue are just built-in
rows** — one filter engine, no special cases.

`filterJson` serializes `ViewFilter` from `:core`: board IDs, status categories,
tag IDs, item types, `dueWithinDays`, `overdueOnly`, `minPriority`,
`includeArchived`, `sortBy`, free-text `query`.

### `sync_outbox`

`id` autoincrement `Long` (local-only, never synced) · `entityType` ·
`entityId` · `opType` · `payloadJson: String?` · `createdAt` · `attemptCount` ·
`lastError: String?`

Created empty in v1 and unused until sync ships. One `CREATE TABLE` now avoids a
migration later.

### `app_meta`

Single row: `deviceId` (UUID, minted on first run) · `lastSyncAt: Long?` ·
`syncCursor: String?`

---

## Query objects (not tables)

- `BoardWithStatuses` — `@Relation`
- `BoardSummary` — `@DatabaseView`: board plus per-category item counts, so the
  home screen doesn't load items to draw a progress bar
- `ItemDetail` — `@Embedded` item + tags, checklist, links, feature-request meta
- `ItemCard` — lean projection for lists. **Never load `notes` into a kanban
  column.**

## Migration history

| Version | Date | Change |
|---|---|---|
| 1 | *pending* | Initial schema |

Search is `LIKE`-based in v1. An FTS4 table over `items(title, notes)` is
planned as v2, partly to exercise the migration path deliberately before a
migration is ever needed under pressure.
