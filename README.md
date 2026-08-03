# ProjectMate

An offline-first Android app for tracking the projects you're building **and**
the real-life things you need to get done — in one place, without either one
being an afterthought.

ProjectMate is built around **collections**: boards you create from templates,
each with its own set of statuses. A "Projects" board and a "Life" board are
just two collections with different status sets, so you're never fighting a tool
that only understands one kind of work.

> **Why it exists, what it refuses to become, and where it's going:**
> [`docs/PURPOSE.md`](docs/PURPOSE.md) and [`docs/ROADMAP.md`](docs/ROADMAP.md).

---

## What it does

- **Collections (boards)** — create as many as you like, from templates
  (Projects, Life, Feature Requests, Simple) or from scratch.
- **Per-board statuses** — every board defines its own, editable at any time:
  rename, recolour, reorder, add, remove.
- **Status categories** — each status is tagged `Backlog` / `Active` /
  `Blocked` / `Done` / `Cancelled`, so progress bars and cross-board views keep
  working no matter what you call your columns.
- **Today** — one screen pulling together what's overdue, due today, and sitting
  in a focus status, across every board.
- **Inbox** — a landing zone for things shared into the app or imported, waiting
  to be triaged.
- **Items** — notes, priority, due dates, tags, and checklists.
- **Import / export** — your whole database as JSON, plus an import path for
  feature requests coming from a website.
- **Fully offline** — no account, no network required, no data leaves the device.

## Status

**Phase 1 — in active development.** See
[`CHANGELOG.md`](CHANGELOG.md) for what has actually landed and
[`docs/ROADMAP.md`](docs/ROADMAP.md) for what's next.

## Getting the app

### Download a build (no toolchain needed)

Every push to the working branch produces an installable debug APK:

1. Open the repository's **Actions** tab
2. Click the most recent successful **Build** run
3. Download the `projectmate-debug-apk` artifact
4. Unzip it and open the `.apk` on your phone
   (you'll need to allow installs from your browser or file manager)

### Build it yourself

Requires JDK 17+ and the Android SDK (Android Studio installs both).

```bash
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/
./gradlew :core:test           # pure-JVM logic tests, no Android SDK needed
./gradlew test lintDebug       # everything
```

## Project layout

```
core/     Pure Kotlin. Domain models, board templates, Today rules,
          import/export schemas. No Android dependencies — testable anywhere.
app/      The Android app. Room storage, Compose UI, ViewModels.
docs/     Intent, architecture, data model, roadmap, infrastructure costs.
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for why it's split this way.

## Documentation

| Document | What's in it |
|---|---|
| [`docs/PURPOSE.md`](docs/PURPOSE.md) | What this tool is for, its principles, and its non-goals |
| [`docs/SITEMAP.md`](docs/SITEMAP.md) | Every screen and sheet, with build status — the UI design reference |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Phased plan — what's shipped, what's next |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module split, layers, data flow |
| [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) | Every table and field, and why |
| [`docs/FEATURE_REQUEST_SCHEMA.md`](docs/FEATURE_REQUEST_SCHEMA.md) | The JSON contract between the website and the app |
| [`docs/INFRASTRUCTURE.md`](docs/INFRASTRUCTURE.md) | Hosting plan for the feature-request portal, with real costs |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Decision log — what was chosen and why |

## License

Personal project. All rights reserved for now.
