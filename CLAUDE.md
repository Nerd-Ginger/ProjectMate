# Working in this repository

Conventions for anyone — human or agent — making changes here.

## The documentation rule

**Any change in behaviour updates the matching document in the same commit.**

This project exists partly to stay legible to its own author months later, so
the docs are treated as part of the deliverable, not as cleanup work.

| If you change… | Update… |
|---|---|
| What the app is for, or what it deliberately won't do | `docs/PURPOSE.md` |
| A Room entity, field, or migration | `docs/DATA_MODEL.md` |
| Module boundaries, layering, or data flow | `docs/ARCHITECTURE.md` |
| The feature-request JSON | `docs/FEATURE_REQUEST_SCHEMA.md` |
| Hosting, or anything with a monthly cost | `docs/INFRASTRUCTURE.md` |
| A choice a future reader would question | `docs/DECISIONS.md` (append an entry) |
| Anything user-visible | `CHANGELOG.md` (under `Unreleased`) |

Shipping a feature also means ticking it off in `docs/ROADMAP.md`.

## Build environment

This repo gets worked on from two very different places, and they have opposite
capabilities. **Work out which one you're in before believing anything about
what you can build.**

| | `./gradlew :core:test` | Android build (`:app`) |
|---|---|---|
| **A machine with Android Studio** | works | **works** — build and run on a device directly |
| **A cloud container** (no SDK, `dl.google.com` blocked) | works | **CI only** |

`:core` is pure Kotlin and resolves entirely from Maven Central, so it tests
anywhere. Everything else depends on the Android SDK.

### `:app` is conditionally included — this will confuse you once

`settings.gradle.kts` includes `:app` **only** when one of these is true:

- `ANDROID_HOME` is set
- `ANDROID_SDK_ROOT` is set
- `local.properties` exists in the repo root
- `PROJECTMATE_FORCE_ANDROID=true`

None of them present and `:app` is silently not part of the build.
`./gradlew :app:assembleDebug` then fails with **"project not found"**, which
reads like a broken checkout rather than a missing SDK. Gradle does log
`No Android SDK detected — skipping :app` during configuration, but it scrolls
past easily. If `:app` seems to have vanished, check the SDK before you check
anything else. (See `docs/DECISIONS.md` D-006.)

### On a machine with Android Studio

Build and run it. Don't route work through CI that you can validate in seconds
on a device — and don't trust a screen you've only compiled. See
`docs/LOCAL_SETUP.md` for setup and an acceptance walkthrough.

### In a cloud container

- Everything Android — compiling, Room codegen, lint, the APK — **only happens
  in GitHub Actions**. CI is the compiler.
- Put logic worth testing in `:core` wherever it's reasonable. It's the only
  place with a fast feedback loop.
- Expect to iterate on CI failures by reading job logs. Keep pushes small so a
  red build points at a small diff.
- **Read the literal error text before forming a theory about it.** A confident
  wrong diagnosis costs a full CI round-trip here; the error has twice already
  named the actual cause while a plausible-sounding guess did not.

## Module boundaries

- **`:core`** — pure Kotlin/JVM. Domain models, board templates, Today rules,
  import/export schemas and logic. **No Android imports, ever.** If you're
  tempted to add one, the logic belongs in `:app` instead.
- **`:app`** — Android. Room persistence, Compose UI, ViewModels, DI. Depends on
  `:core`; `:core` never depends on `:app`.

## Code conventions

- Kotlin, 4-space indent, 120-column soft limit (`.editorconfig`).
- Compose + Material 3. Single Activity, Navigation Compose.
- MVVM: ViewModel exposes `StateFlow`, DAOs return `Flow`, UI is stateless where
  it can be.
- **Manual DI via `AppContainer`** — not Hilt. Deliberate; see
  `docs/DECISIONS.md`. Don't add an annotation processor without a reason.
- IDs are client-generated UUID strings. Never autoincrement integers — the sync
  work in phase 2 depends on this.
- Deletes are soft (`deletedAt`), not `DELETE`. Queries must filter tombstones.
- Room schemas are exported to `app/schemas/` and **committed**. Bumping the DB
  version means writing a migration and a migration test.

## Commits

- Conventional-commit prefixes: `feat:`, `fix:`, `docs:`, `build:`, `ci:`,
  `chore:`, `refactor:`, `test:`. Scope where useful — `feat(core):`.
- Commit in coherent increments. Each commit should leave the tree in a state
  that at least intends to build.
- **Never write `[skip ci]` in a commit message unless you mean it.** GitHub
  matches that token anywhere in the message, including prose. A commit that
  merely *described* the schema-export step silently skipped its own build. A
  skipped build looks exactly like a passing one — and where CI is the only
  compiler, nothing else would have caught it. Refer to it as "the skip-ci
  marker" instead.
- Work happens on `claude/android-project-tracker-bizgrj`. Never push to `main`
  without being asked.

## Before you call something done

1. `./gradlew :core:test` if `:core` changed
2. **If you can run the app, run it.** Compiling is not evidence that a screen
   works. Anything user-visible gets looked at on a device before it's "done"
3. Push and confirm the CI run goes green — a red build is not "done"
4. Docs updated per the table above
5. `CHANGELOG.md` has an entry if a user would notice the change
