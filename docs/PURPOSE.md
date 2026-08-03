# Purpose

*The intent behind ProjectMate. If a proposed feature can't be justified against
this document, it doesn't belong in the app — or this document needs to change
first.*

Last reviewed: 2026-08-03

---

## The problem

Two kinds of work compete for the same attention and don't live in the same
place:

1. **Projects** — the things being built. They have phases, they stall waiting
   on something, they get parked and picked back up months later.
2. **Life** — the things that just need doing. Errands, admin, obligations.
   They're rarely "in review"; they're either today's problem or they aren't.

Splitting these across two apps means neither gives an honest answer to *"what
actually needs my attention right now?"* Combining them in a tool that only
models one shape of work means the other gets crammed into columns that don't
fit.

Nothing off-the-shelf solves this at an acceptable price for a single person.
Project tools charge per-seat for collaboration that isn't needed here; task
apps can't express a project stalled in review.

## What ProjectMate is

**A single, private, offline-first tracker built around collections.**

A *collection* is a board you create. It owns its own set of statuses. That one
idea is what lets projects and life coexist without compromise: a Projects board
runs `Idea → Planned → In Progress → Blocked → In Review → Shipped → Parked`,
a Life board runs `Inbox → Today → Next → Waiting On → Someday → Done`, and
neither is a distortion of the other.

Because statuses are per-board and freely editable, the app doesn't need to
predict what you'll track next. A reading list, a house renovation, and a
backlog of feature requests are all just collections.

## Principles

**1. Your columns, your names.**
Statuses are created, renamed, recoloured and reordered by you. The app never
hardcodes a workflow. Templates are a starting point, not a contract.

**2. Cross-board features must not depend on your naming.**
Every status carries a *category* — `Backlog`, `Active`, `Blocked`, `Done`,
`Cancelled` — and an optional *focus* flag. Progress bars, "hide completed" and
the Today view read the category, never the label. Rename "Shipped" to
"Launched" and nothing breaks. This is the mechanism that makes principle 1 safe.

**3. Offline first, and offline *sufficient*.**
The app is fully usable with no network, no account, and no sign-in. Sync, when
it arrives, is an optional addition — never a prerequisite for opening the app.

**4. Your data stays yours and stays portable.**
Everything exports to readable JSON, and imports back. No lock-in. If this
project is abandoned, the data survives it.

**5. One honest answer to "what now?".**
The Today view aggregates across every board: overdue, due today, and anything
in a focus status. If that screen is wrong or noisy, that's a bug, not a
preference.

**6. Capture must be frictionless.**
Anything shared into the app lands in the Inbox without demanding a board, a
status, or a due date up front. Triage is a separate act from capture.

**7. Built for one person.**
Single-user by design. No permissions model, no assignees, no team semantics —
their absence is what keeps the app fast and simple.

## Non-goals

Stated explicitly, so they can be argued with rather than drifted into:

- **Not a team tool.** No accounts, no sharing, no assignees, no comment
  threads. The one multi-person surface planned is a *public feature-request
  form* whose submissions arrive as items — visitors never get access to the app.
- **Not time tracking.** No timers, no billable hours, no timesheets.
- **Not a calendar.** Items have due dates; the app does not schedule your day
  or replace a calendar.
- **Not a notes app.** Items carry notes, but ProjectMate is not where long-form
  writing lives.
- **Not automation.** No rules engines, no triggers, no workflow builder. That
  complexity is what makes the alternatives unpleasant.
- **Not cloud-dependent.** No feature will ever require a server to function.
- **Not a Git or issue-tracker client.** It may *receive* items from elsewhere;
  it doesn't mirror GitHub.

## Who it's for

One person — the author — tracking their own projects and their own life, on
their own phone. Every design tension resolves in favour of that person's daily
use, not hypothetical future users.

The one exception: strangers on a website who want to request a feature. They
interact with a form, never with the app.

## How success is measured

- It gets opened daily, without being a chore.
- The Today view is trusted enough that nothing else needs checking.
- Adding an item takes seconds, not deliberation.
- A project parked for three months can be resumed without re-reading anything.
- Feature requests from the website land in the Inbox and get triaged, rather
  than being lost in an email folder.

---

## Keeping this document honest

This is a living document, reviewed whenever a feature lands that changes what
the tool *is* — not just what it does.

- Adding a capability that stretches the definition above → update **What
  ProjectMate is**.
- Deciding against something on principle → add it to **Non-goals**.
- Discovering a principle is being violated in practice → fix the app, or
  change the principle deliberately and say why in `DECISIONS.md`.

Update the *Last reviewed* date whenever this file is meaningfully revised.
