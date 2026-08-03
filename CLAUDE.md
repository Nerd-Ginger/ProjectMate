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

**The development container cannot build Android.** `dl.google.com` — which
serves both the Android SDK and Google's Maven repository — is blocked by
network policy, and no SDK is installed. Verified, not assumed.

What this means in practice:

- `./gradlew :core:test` **works locally**. The `:core` module is pure Kotlin
  and resolves entirely from Maven Central.
- Everything Android — compiling, Room codegen, lint, the APK — **only happens
  in GitHub Actions**. CI is the compiler.
- Put logic worth testing in `:core` wherever it's reasonable. It's the only
  place with a fast feedback loop.
- Expect to iterate on CI failures by reading job logs. Keep pushes small so a
  red build points at a small diff.

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
- Work happens on `claude/android-project-tracker-bizgrj`. Never push to `main`
  without being asked.

## Before you call something done

1. `./gradlew :core:test` locally if `:core` changed
2. Push and confirm the CI run goes green — a red build is not "done"
3. Docs updated per the table above
4. `CHANGELOG.md` has an entry if a user would notice the change
