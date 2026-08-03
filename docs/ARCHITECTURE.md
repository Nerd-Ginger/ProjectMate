# Architecture

Last updated: 2026-08-03

---

## Modules

Two, and the split is deliberate.

```
:core   Pure Kotlin/JVM. No Android, no AndroidX — the build file would
        not resolve them even if you tried.
        Domain models · status semantics · board templates · sort keys ·
        UUIDv7 · due-date rules · view filters · import/export schemas.

:app    Android. Room persistence · Compose UI · ViewModels · manual DI ·
        intent handling. Depends on :core. :core never depends on :app.
```

The architectural argument for a domain layer that can't reach for a `Context`
is real but secondary. The primary reason is mechanical: **`:core` compiles and
tests in the dev container and `:app` does not** — Google's Maven repo is
unreachable there (see [`DECISIONS.md`](DECISIONS.md) D-006). Anything whose
correctness isn't obvious by inspection belongs in `:core`, where the feedback
loop is seconds instead of a CI round-trip.

Package root: `com.nerdginger.projectmate`.

## Layers

```
Compose UI  ──observes──►  ViewModel  ──calls──►  Repository  ──►  DAO  ──►  Room
    │                        (StateFlow)            (Flow)         (Flow)
    └──────────────── events / user intents ─────────────────────────►
```

- **UI** is stateless where it can be. Screens receive state plus lambdas.
- **ViewModel** owns screen state as `StateFlow`, exposes suspend actions.
  Created via `viewModelFactory { initializer { … } }` reading `AppContainer`.
- **Repository** maps entities ↔ `:core` models and is the only thing UI code
  is allowed to talk to. It never leaks Room types upward.
- **DAO** returns `Flow` for reads and is `suspend` for writes.

Data flows one way. Nothing above the repository knows Room exists; nothing in
`:core` knows Android exists.

## Dependency injection

Hand-written `AppContainer`, held by the `Application`, no Hilt. Roughly ten
objects — a database, its DAOs, a handful of repositories, DataStore
preferences, the import service. See [`DECISIONS.md`](DECISIONS.md) D-004 for
why an annotation processor isn't worth it here.

## Navigation

**No navigation library.** A single Activity holds a back stack as a
`SnapshotStateList<Screen>` where `Screen` is a sealed interface; `BackHandler`
pops it. That's ~60 lines and it is, structurally, what Navigation 3 does.

The reason is the same constraint as everywhere else: an unresolvable artifact
coordinate costs a full CI round-trip to discover, and navigation is the one
place where the library buys almost nothing for a single-Activity app. Pushing
the import screen from a share intent is `backStack.add(ImportPreview(payload))`
— no graph, no route strings, no argument encoding.

Top level is a `NavigationSuiteScaffold` (bottom bar on phones, rail on wider
screens) with four destinations: **Boards · Today · Inbox · Search**. Everything
else pushes onto the stack.

## Persistence

Room 3 (`androidx.room3`) with `BundledSQLiteDriver`. Two consequences worth
knowing:

- Every DAO function must be `suspend` or return `Flow` — Room 3 rejects
  blocking DAO functions at compile time. Write them that way from the start.
- Because Room 3 is KMP and the bundled driver runs on the JVM, **DAO and
  migration tests run as plain JVM unit tests** — no emulator, no Robolectric.
  Given that CI is the only place Android code builds at all, that matters.

Schemas are exported to `app/schemas/` and committed. A version bump without a
migration fails the build.

## Cross-cutting rules

- **IDs** are client-generated UUIDv7 strings. Time-ordered, so they index well
  and carry a natural creation order.
- **Deletes are soft.** Every query filters `deletedAt IS NULL`. A hard delete
  has nothing to tell a future server about, so rows would resurrect on sync.
- **Ordering** uses fractional string sort keys (LexoRank-style). Dragging a
  card is a single-row `UPDATE`, not a renumber of the column, and two devices
  reordering concurrently merge deterministically.
- **Status categories, not status names.** Anything working across boards reads
  `StatusCategory`, never a label. See [`DECISIONS.md`](DECISIONS.md) D-002.
- **One JSON parser.** Share intent, file open, and the future Worker pull all
  hand a `String` to the same parser. See
  [`FEATURE_REQUEST_SCHEMA.md`](FEATURE_REQUEST_SCHEMA.md).

## Toolchain

| Piece | Version |
|---|---|
| Android Gradle Plugin | 9.3.0 |
| Gradle (wrapper) | 9.5 |
| Kotlin | 2.4.10 |
| KSP | 2.3.10 |
| Compose BOM | 2026.06.01 |
| Room | 3.0.1 |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| Java bytecode target | 17, compiled by JDK 21 |

**No `jvmToolchain {}` block.** The container has only JDK 21, and toolchain
auto-provisioning is another network dependency that may be blocked. Setting
`compileOptions` and `jvmTarget` to 17 emits compatible bytecode from whatever
JDK runs Gradle, which removes a failure mode.

`minSdk 26` gives `java.time` natively — no desugaring, no date-time library.

## Testing

| Runs where | What |
|---|---|
| Dev container + CI | `:core` unit tests — sort keys, UUIDv7, due-date rules, templates, JSON parse/round-trip |
| CI only | Room DAO + migration tests (JVM, bundled driver), ViewModel tests, `lintDebug`, `assembleDebug` |
| Device | The manual checklist in [`ROADMAP.md`](ROADMAP.md) acceptance criteria |
