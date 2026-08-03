# Feature-request JSON schema

**This file is the contract between the website and the app.** Whoever writes
the Cloudflare Worker implements this document; the app already parses it.

Schema version: **1**
Last updated: 2026-08-03

---

## Why this exists before the portal does

The app ships file import and share-target import in phase 1. The phase-2 Worker
will serve *exactly the same envelope* from `GET /api/requests`. One schema, one
parser, one set of tests — phase 2 becomes a transport change rather than a
feature.

It also means manual file import stays a permanent fallback for when the
network, the token, or the Worker is having a bad day, and that an export from
any other system can be reshaped into this and imported.

---

## Envelope

```json
{
  "schemaVersion": 1,
  "kind": "projectmate.featureRequests",
  "generatedAt": "2026-08-03T14:22:00Z",
  "source": {
    "type": "web-portal",
    "siteId": "nerdginger.com",
    "exportId": "exp_01J9XKQ2R8ZP4M7T0YB3C6WE"
  },
  "cursor": {
    "since": "2026-07-27T00:00:00Z",
    "next": "eyJvZmZzZXQiOjUwfQ"
  },
  "requests": [
    {
      "id": "fr_01J9XKQ2R8ZP4M7T0YB3C6WE",
      "projectSlug": "projectmate",
      "title": "Dark mode for the kanban board",
      "body": "Long-form description. Markdown is allowed.",
      "type": "feature",
      "priorityHint": "normal",
      "submittedAt": "2026-08-01T10:04:11Z",
      "updatedAt": "2026-08-02T09:00:00Z",
      "status": "new",
      "votes": 3,
      "tags": ["ui", "android"],
      "requester": {
        "name": "Sam",
        "email": "sam@example.com",
        "contactOptIn": true
      },
      "url": "https://nerdginger.com/requests/fr_01J9XKQ2R8ZP4M7T0YB3C6WE",
      "meta": { "turnstileVerified": true, "countryCode": "US" }
    }
  ]
}
```

### Envelope fields

| Field | Type | Required | Notes |
|---|---|---|---|
| `schemaVersion` | int | ✅ | see compatibility rules |
| `kind` | string | ✅ | `projectmate.featureRequests` or `projectmate.backup` |
| `generatedAt` | ISO-8601 UTC | ✅ | |
| `source.type` | string | ✅ | `web-portal` · `manual` · `export` |
| `source.siteId` | string | — | which site produced it |
| `source.exportId` | string | — | for tracing |
| `cursor.since` | ISO-8601 | — | what the export covers |
| `cursor.next` | opaque string | — | pagination token; the app stores it and sends it back |
| `requests` | array | ✅ | may be empty |

### Request fields

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | ✅ | **opaque and stable forever.** The dedup key |
| `projectSlug` | string | ✅ | routes to a board via that board's `portalSlug` |
| `title` | string | ✅ | 1–200 chars |
| `body` | string | — | markdown |
| `type` | enum | — | `feature` · `bug` · `question` · `other` (default `feature`) |
| `priorityHint` | enum | — | `low` · `normal` · `high` (a request, not a promise) |
| `submittedAt` | ISO-8601 | ✅ | |
| `updatedAt` | ISO-8601 | — | drives update-vs-skip on re-import |
| `status` | string | — | portal-side status; advisory only |
| `votes` | int | — | default 0 |
| `tags` | string[] | — | created on import if new |
| `requester.name` | string | — | |
| `requester.email` | string | — | **personal data** — see below |
| `requester.contactOptIn` | bool | — | default false |
| `url` | string | — | back-link to the request on the site |
| `meta` | object | — | free-form; stored verbatim, not interpreted |

---

## Rules the Worker must honour

**1. `id` is opaque, stable and unique, forever.**
It becomes `items.externalRequestId`, which has a unique index. Re-importing the
same payload must be a no-op, not a pile of duplicates.

**2. Additive changes only within a schema version.**
The app parses with `ignoreUnknownKeys = true`, so new fields are safe to add
without an app update. Removing or retyping a field requires a version bump.

**3. Bump `schemaVersion` for breaking changes.**
The app refuses payloads with a higher version than it understands and says so
plainly, rather than silently dropping data.

**4. Timestamps are ISO-8601 with an explicit UTC offset.**
`2026-08-01T10:04:11Z`. Not epoch seconds, not local time.

**5. Keep exports bounded.** Page with `cursor.next`. The app rejects payloads
over 5 MB.

---

## How the app handles it

**Parsing is non-fatal per row.** The parser returns
`ParseResult(valid, rejected)` — one malformed request never kills a 200-request
import. Rejections carry a reason and are shown in the import preview.

**Nothing imports silently.** Every path lands on an import preview screen
showing *N new · M updates · K rejected*, with the target board and landing
status selectable, before anything is written. The whole batch then commits in
one transaction.

**Routing:** `projectSlug` → the board whose `portalSlug` matches → items land
in that board's `portalDefaultStatusId`. No match? The preview asks once and
offers to remember it.

**Mapping:** each request becomes an `items` row with
`itemType = FEATURE_REQUEST`, `origin = WEB_PORTAL`,
`externalRequestId = id`, `createdAt = submittedAt`, plus a
`feature_request_meta` row holding the requester details, votes, and the
**verbatim original JSON**. Tags are created if they don't exist.

**Re-import:** matched on `externalRequestId`. A newer `updatedAt` refreshes the
request's **portal-owned metadata only** — votes, requester details, and the
stored raw payload. Anything else is skipped.

Title, notes and status are never touched by a re-import. The portal is a source
of new requests, not the owner of your triage: a re-import that silently
reverted a retitle or a status change would make the app untrustworthy. This is
structural rather than a rule to remember — the update path carries no `Item` at
all, so there is nothing to clobber with.

---

## Transports

All four hand the same `String` to the same parser:

| | When |
|---|---|
| **Share intent** — `ACTION_SEND` `text/plain` or `application/json` | phase 1 |
| **File open** — `ACTION_VIEW`, SAF picker, `*.json` / `*.pmjson` | phase 1 |
| **Deep link** — `projectmate://import?url=…` | phase 1 |
| **Worker pull** — `GET /api/requests?since=<cursor>` | phase 2 |

---

## Privacy

`requester.email` is personal data.

- The `GET` endpoint **must** require a secret token (a Worker secret, never in
  the repo or the app source).
- The form must say what's collected and why.
- Ask for the minimum. An email is only useful if `contactOptIn` is true —
  consider not collecting it otherwise.
- Backup exports contain these addresses. Treat an exported backup as sensitive.

Infrastructure and costs for the Worker: [`INFRASTRUCTURE.md`](INFRASTRUCTURE.md).

---

## Backup envelope

Full-app export reuses these conventions with `"kind": "projectmate.backup"` and
one array per table, including all sync fields — so a backup restored onto a new
device keeps its UUIDs and can still sync later.

Filename: `projectmate-backup-YYYY-MM-DD.json`
