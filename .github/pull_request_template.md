## What this changes

<!-- One or two sentences. What can the app do now that it couldn't before? -->

## Why

<!-- The problem or need. Link an item in docs/ROADMAP.md if there is one. -->

## Verification

<!-- What you actually ran, and what you couldn't. Be specific. -->

- [ ] `./gradlew :core:test` passes
- [ ] CI is green (`:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`)
- [ ] Checked on a device — or explicitly noted below that it wasn't

## Documentation

Per the rule in `CLAUDE.md`, behaviour changes update their matching doc in the
same commit.

- [ ] `docs/DATA_MODEL.md` — entity, field or migration changed
- [ ] `docs/ARCHITECTURE.md` — module boundaries, layering or data flow changed
- [ ] `docs/FEATURE_REQUEST_SCHEMA.md` — the JSON contract changed
- [ ] `docs/INFRASTRUCTURE.md` — anything with a recurring cost changed
- [ ] `docs/PURPOSE.md` — what the tool is for, or won't do, changed
- [ ] `docs/DECISIONS.md` — a choice a future reader would question
- [ ] `docs/ROADMAP.md` — roadmap item ticked off
- [ ] `CHANGELOG.md` — user-visible change recorded
- [ ] N/A — nothing user-visible or structural changed

## Notes

<!-- Anything deliberately left out, known rough edges, follow-up work. -->
