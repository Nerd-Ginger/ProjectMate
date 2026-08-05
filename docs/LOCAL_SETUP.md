# Local setup

Getting ProjectMate building, running, and being worked on from a machine with
Android Studio.

**Why this document exists:** every commit so far was written in a cloud
container with no Android SDK and `dl.google.com` blocked. GitHub Actions has
been the only compiler, and **the app has never been executed** — not on a
device, not on an emulator. It compiles and its `:core` logic is tested, but no
screen has ever been looked at.

A machine with Android Studio fixes that. The most valuable thing to do first is
not to add a feature, but to run what already exists and find out what's broken.

---

## 1. Prerequisites

| | |
|---|---|
| **JDK** | 17 or newer. CI uses Temurin 21. Bytecode target is 17 either way (`docs/DECISIONS.md` D-012) |
| **Android Studio** | For the SDK, and for a device or emulator to run on |
| **compileSdk / targetSdk** | 37 |
| **minSdk** | 26 |

There is no Gradle toolchain block, deliberately — Gradle compiles with whatever
JDK runs it. Any 17+ JDK works; you don't need exactly 17 or exactly 21.

```bash
git clone https://github.com/Nerd-Ginger/ProjectMate.git
cd ProjectMate
git checkout claude/android-project-tracker-bizgrj
```

Then open the project in Android Studio once. It writes `local.properties`
pointing at your SDK, which is what makes `:app` part of the build.

**If `ANDROID_HOME` or `ANDROID_SDK_ROOT` is already set in your environment,
that alone is enough** — `local.properties` is one of four accepted signals, not
a hard requirement. Opening in Android Studio is still the easiest path, and it
generates the file regardless.

**Never commit `local.properties`.** It hardcodes an absolute SDK path specific
to your machine. It's already in `.gitignore`; leave it that way.

---

## 2. First build

Start with the module that needs nothing:

```bash
./gradlew :core:test
```

85 tests, ~30 seconds once Gradle has warmed up, no Android SDK involved. If
this fails, the problem is your JDK, not your SDK.

Then the app:

```bash
./gradlew :app:assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`. Or just hit Run in Android Studio.

### If `:app:assembleDebug` says "project not found"

That is **not** a broken checkout. `settings.gradle.kts` includes `:app` only
when it can detect an SDK — via `ANDROID_HOME`, `ANDROID_SDK_ROOT`,
`local.properties`, or `PROJECTMATE_FORCE_ANDROID=true`. With none of them,
`:app` silently isn't part of the build at all.

Gradle does say so during configuration:

```
No Android SDK detected — skipping :app. Only :core will be configured.
```

but it scrolls past easily and the error you actually notice is the confusing
one. Confirm with:

```bash
./gradlew projects
```

If `:app` isn't listed, fix the SDK detection rather than the checkout.

### If Gradle says "Unable to delete directory"

```
java.io.IOException: Unable to delete directory '…\app\build\intermediates\…'
Failed to delete some children. This might happen because a process has files
open or has its working directory set in the target directory.
```

Gradle's recursive delete fails intermittently and names a **different**
directory each retry. Nothing is actually holding the files — PowerShell removes
the same directory without complaint, which is the tell. Stopping the daemon
doesn't fix it.

```bash
./gradlew --stop && rm -rf app/build core/build build
```

then re-run; a clean build gets past it.

This checkout lives on `Z:`, which is a **local NTFS volume** (`DriveType 3`),
not a network share — so the usual "it's the network drive" explanation does not
apply. The likely culprit is a real-time file scanner briefly holding handles
under `build/`. If it becomes a nuisance, adding the repo to your antivirus
exclusions is a better fix than moving the checkout.

---

## 3. Acceptance walkthrough

**Run in full on a Pixel 5 (Android 14) on 2026-08-04** — the first time any of
this code had ever executed. All eight steps now pass. It found four real bugs,
all fixed in the same commit: lost navigation state on rotation, a top bar that
said "Board" instead of the board's name, shrink-wrapped kanban columns, and an
off-palette selection colour in the template sheet.

Re-run it after any change to navigation, persistence, or the board screens. It
is still a bug-finding exercise rather than a demo — treat a failure as the
work, not as an interruption to it.

Run it on a real device or emulator, top to bottom:

1. **First launch.** The Boards home is the start destination. A pinned **Inbox**
   collection is seeded automatically; no other boards are created — that's
   deliberate (`DatabaseSeeder.seedIfEmpty`), not a bug. The other three tabs —
   **Today**, **Inbox**, **Search** — should each show a named "Not built yet"
   placeholder.
2. **FAB → Projects template.** The template picker should show the statuses the
   template defines before you commit. Create the board.
3. **Open the board.** Kanban columns appear in the template's own order, not
   alphabetical and not reordered.
4. **Add items inline**, then use the advance button to move one all the way to
   the final column.
5. **Back to home.** The board card's segmented progress bar should reflect the
   moves you just made — segments are `Backlog / Active / Blocked / Done` by
   status *category*, not a percentage.
6. **Create a Life board.** Both boards coexist, each with its own status set.
   Confirm the Life statuses are genuinely different from the Projects ones —
   per-board statuses are the core premise of the app.
7. **Kill the app and relaunch.** Everything persists. Nothing is re-seeded and
   no duplicate Inbox appears.
8. **Rotate the device** on the board view. No crash, no lost scroll position,
   no lost in-progress text in the inline add field.

Anything that fails here is worth more than the next feature. Fix it, and add a
`:core` test if the cause turns out to be logic that could have lived there.

---

## 3a. The design

The UI is transcribed from a design comp, committed at
**`design/ProjectMate.dc.html`** so it travels with the code. Open it in a
browser — it renders 16 screens and 7 sheets with a clickable index, and the
styles are inline, so the exact value for anything is readable straight off the
element.

What is already transcribed into
`app/src/main/kotlin/.../designsystem/Theme.kt`:

| | |
|---|---|
| Canvas | `#0B0B0C` |
| Cards / chips | `#141419`, raised `#1A1A20`, high `#26262E` |
| Accent | `#FF6B1A` (bright `#FF8A4C`) |
| Text | `#F2F0EE` → `#93908C` → `#7E7B77` → `#6E6B67` |
| Blocked | `#E2453C`, label `#FF7A70` |
| Categories | backlog `#3A3A42` · active accent · blocked red · done `#6F6C68` |

Two deliberate departures from the comp, both worth knowing before you "fix"
them:

- **Monospace is `FontFamily.Monospace`, not JetBrains Mono.** That resolves to
  Roboto Mono on Android and needs no font binary in the APK. Swap in the real
  face if the difference ever matters.
- **The smart-view chips on the Boards home** (Today / Inbox / Overdue with
  counts) are in the comp but not built — they need cross-board queries that
  belong with the Today screen.

Screens 6 onward in the comp — item detail, list and agenda views, the status
editor, settings — have no implementation yet. The comp is the target; check it
before designing anything new.

## 4. Working on it locally

- The working branch is `claude/android-project-tracker-bizgrj`.
- **`main` is not pushed to without being asked.** When you want the work on
  `main`, open a PR:
  ```bash
  gh pr create --base main --head claude/android-project-tracker-bizgrj
  ```
  or open the compare view on GitHub. Nothing merges itself.
- CI still runs on every push to every branch, and still builds the APK. It's
  useful as a second opinion — it just isn't your only compiler any more.
- The documentation rule in `CLAUDE.md` applies exactly the same locally.

---

## 5. Kickoff prompt

Paste this into a fresh Claude Code session on the local machine.

```text
This is ProjectMate — an offline-first Android app for tracking software
projects and real-life tasks in one place, built around per-board custom
statuses. Read docs/PURPOSE.md, docs/SITEMAP.md, docs/DECISIONS.md and
CLAUDE.md before changing anything.

IMPORTANT — your environment differs from where this code was written. Every
commit so far came from a cloud container with no Android SDK, where GitHub
Actions was the only compiler and the app was never once executed. You are on
a machine with Android Studio: you can build and run on a device right now.
Use that. Do not route work through CI that you can verify in seconds locally,
and do not trust a screen you have only compiled.

Work on the branch claude/android-project-tracker-bizgrj. Do not push to main
without being asked.

THE UI FOLLOWS A DESIGN COMP, committed at design/ProjectMate.dc.html. Open it
in a browser — 16 screens and 7 sheets, clickable index, inline styles you can
read exact values from. The theme, Boards home and kanban are
already transcribed; everything from screen 6 on is not built. Read section 3a
of docs/LOCAL_SETUP.md, and check the comp before designing anything new.

FIRST TASK, in this order, per docs/SITEMAP.md: item detail (notes, priority,
due date, tags, checklist) → Today (the cross-board attention view, and the
screen the app is judged on) → Inbox plus the share-target and file-import
paths. Build each to match the comp.

RUN THE APP, DON'T JUST COMPILE IT. The acceptance walkthrough in section 3 of
docs/LOCAL_SETUP.md passes as of 2026-08-04, and running it is what found the
four bugs fixed that day — rotation losing the back stack, a top bar showing
"Board" instead of the board name, shrink-wrapped columns, an off-palette
selection. Re-run it after touching navigation, persistence or the board
screens. `adb` drives it fine: `adb shell input tap x y` plus
`adb exec-out screencap` is enough to walk the whole thing.

TWO OPEN PRODUCT QUESTIONS, both yours to raise rather than silently decide:
- Seeding. First run currently creates only the system Inbox; starter Projects
  and Life boards are meant to be offered in onboarding, which does not exist
  yet. Decide whether onboarding gets built or the empty state does the job.
- Drag-and-drop. Board columns advance via a one-tap button today. Sort keys
  already support arbitrary reordering, so drag-and-drop is a UI change rather
  than a data change — but it is not scheduled.

WORKING HABIT THAT EARNED ITS KEEP: when a build fails, read the literal error
text before forming a theory about it. It has named the cause every time so
far — "no sqliteJni in java.library.path" was a missing JVM native artifact,
not a Room problem; "expected Saver<T, List<String>>, actual Saver<T, Any>" was
listSaver's real return type. Guessing costs more than reading.
```
