# Decision log

Short records of choices a future reader might otherwise undo by accident.
Append; don't rewrite. When a decision is reversed, add a new entry that
supersedes the old one rather than editing history.

Format: what was decided, what else was considered, and why.

---

## D-001 — Collections with per-board statuses, not fixed page types

**2026-08-03 · Accepted**

The app needs to track both projects and personal tasks. Rather than building a
"Projects" screen and a "Tasks" screen, everything is a *collection* — a board
that owns its own ordered set of statuses.

**Considered:** two hardcoded sections, each with its own model.

**Why:** hardcoding two shapes means a third (feature requests, a reading list,
a renovation) needs new code every time. Per-board statuses cost roughly the
same to build and cover every future case. The templates system gives the
"projects page" and "life page" experience on first launch without either being
special.

---

## D-002 — Statuses carry a category, and cross-board features read the category

**2026-08-03 · Accepted**

Each status has a `StatusCategory` (`BACKLOG`, `ACTIVE`, `BLOCKED`, `DONE`,
`CANCELLED`) plus an `isFocus` flag. Progress calculations, "hide completed",
and the Today view read the category — never the status name.

**Considered:** a boolean `isDone` flag; or matching on well-known names.

**Why:** this is what makes fully user-editable statuses safe. Rename "Shipped"
to "Launched" and every cross-board feature keeps working. A boolean `isDone`
can't distinguish blocked-vs-active or cancelled-vs-completed, both of which
matter for an honest Today view. Name-matching breaks the moment the user edits
anything.

---

## D-003 — Client-generated UUIDv7 primary keys and soft deletes, from day one

**2026-08-03 · Accepted**

Every table uses a **UUIDv7** `String` primary key generated on-device, plus an
embedded `SyncMeta` block (`createdAt`, `updatedAt`, `deletedAt`, `remoteId`,
`syncState`, `version`, `origin`) — even though phase 1 has no server at all.

**Considered:** `@PrimaryKey(autoGenerate = true) Long`; UUIDv4; adding the sync
fields later when they're actually needed.

**Why:** sync is on the roadmap (phase 2 pulls feature requests, phase 3 syncs
devices). Autoincrement integers collide across devices and would force a
migration that rewrites every foreign key. Adding the columns later is worse
than it sounds — it means backfilling identities for rows that exist on exactly
one device and nowhere else.

**v7 rather than v4** because v7 is time-ordered: sequential inserts cluster in
the B-tree instead of scattering, and the key carries an implicit creation order
for free.

`version` earns its place separately from `updatedAt`: last-write-wins on a
timestamp silently loses an edit when two devices' clocks disagree. A
server-incremented revision makes the conflict *detectable*.

Soft deletes exist for the same reason as the UUIDs — a hard `DELETE` leaves
nothing to tell a server the row is gone, so deleted items resurrect on the next
pull. The price is that every query must filter `deletedAt IS NULL`. Accepted.

---

## D-004 — Manual dependency injection, not Hilt

**2026-08-03 · Accepted**

Dependencies are wired by hand in an `AppContainer` held by the `Application`.

**Considered:** Hilt (the conventional choice), Koin.

**Why:** primarily a build-environment constraint. Android code cannot be
compiled in this development container (see D-006), so every compile error costs
a full CI round-trip. Hilt adds an annotation processor, a Gradle plugin, and a
class of failures that only surface at codegen time. A hand-written container is
~40 lines for an app this size and fails at edit time instead.

Secondarily: single-user app, one Activity, a handful of ViewModels. Hilt's
scoping machinery has nothing to do here.

**Revisit if:** the graph grows past ~15 objects, or WorkManager injection in
phase 2 gets awkward. Swapping in Hilt later is mechanical.

---

## D-005 — A pure-JVM `:core` module separate from `:app`

**2026-08-03 · Accepted**

Domain models, board templates, Today rules, and all import/export logic live in
a `:core` module with no Android dependencies. `:app` holds Room, Compose and
ViewModels.

**Considered:** a single `:app` module, which is the norm at this size.

**Why:** `:core` compiles and tests locally against Maven Central; anything
Android does not (D-006). Putting the logic worth testing in `:core` buys a fast
feedback loop for exactly the code where correctness is non-obvious — import
dedup, template seeding, date rules. The architectural benefit (a domain layer
that can't accidentally reach for `Context`) is real but secondary.

---

## D-006 — CI is the compiler

**2026-08-03 · Accepted**

GitHub Actions builds, lints, tests and packages the APK. The workflow landed
before any feature code.

**Why:** the development container has no Android SDK, and `dl.google.com` —
serving both the SDK and Google's Maven repository — is blocked by network
policy. Verified, not assumed. Android code therefore cannot be compiled locally
at all, and CI is the only place a build is ever validated.

**Consequences:** the CI workflow is load-bearing, not a nicety. Commits stay
small so a red build points at a small diff. Logic goes in `:core` where it can.
CI also produces the sideloadable APK, which removes any need for Android Studio
on the user's side.

---

## D-007 — Cloudflare Workers + D1 for the portal; no VPS

**2026-08-03 · Accepted**

The phase-2 feature-request portal runs on Cloudflare Pages + Workers + D1 +
Turnstile. No virtual private server.

**Considered:** Hetzner CX22 (~€3.79/mo), Fly.io, Oracle Cloud Always Free.
Costs are in [`INFRASTRUCTURE.md`](INFRASTRUCTURE.md).

**Why:** the workload is a handful of tiny, spiky requests — the free tier is
about three orders of magnitude larger than needed, and the paid ceiling is
$5/month. The deciding factor isn't price (the VPS options are $0–6 too) but
ownership: a server means OS patching, TLS renewal, backups, and downtime you
find out about late. For a one-person project that maintenance is the real cost.

**Revisit if:** long-running background jobs, a real Postgres, or portability
off Cloudflare becomes a requirement.

---

## D-008 — One JSON schema for feature requests, shared by file import and the future API

**2026-08-03 · Accepted**

The website's Worker will serve exactly the JSON that the phase-1 file importer
already parses. See [`FEATURE_REQUEST_SCHEMA.md`](FEATURE_REQUEST_SCHEMA.md).

**Considered:** a purpose-built sync protocol in phase 2, with file import as a
throwaway stopgap.

**Why:** one schema means one parser, one set of tests, and one thing to get
right. Phase 2 becomes a transport change, not a feature. It also leaves manual
file import as a permanent fallback for when the network, the token, or the
Worker is having a bad day — and makes the app usable with a feature-request
export from anywhere, not just this one website.

Imports dedup on a unique `externalRequestId`, so pulling the same request twice
is harmless.

---

## D-009 — Fractional string sort keys, not numeric positions

**2026-08-03 · Accepted**

Anything the user can reorder — cards in a column, statuses, checklist entries —
orders by `sortKey: String`, a LexoRank-style fractional index (`"a0"`, `"a0h"`,
`"a1"`).

**Considered:** an `Int` position (renumber on every move), or a `Double`
position (insert at the midpoint of the neighbours).

**Why:** integers mean rewriting every row in a column to move one card.
Doubles avoid that and are the common tutorial answer, but they fail quietly:
each insert between the same two cards halves the gap, and IEEE-754 runs out of
room after roughly fifty moves — at which point cards start silently sharing a
position. Strings have no such floor; you can always mint a key between two
others.

The concurrency property matters too. Two devices dropping cards in the same
column produce keys that merge deterministically without either edit being lost.

Lives in `:core`, so the tricky part — `between()` — is unit-tested locally,
including the pathological "insert a thousand times between the same pair" case.

---

## D-010 — No navigation library

**2026-08-03 · Accepted**

Navigation is a `SnapshotStateList<Screen>` back stack in the single Activity,
with `Screen` as a sealed interface and `BackHandler` popping it. About 60 lines.

**Considered:** Navigation Compose 2.x (route strings, argument encoding);
Navigation 3 (`androidx.navigation3`), whose model is *also* a caller-owned
back-stack list.

**Why:** Navigation 3 validates the pattern — a list you own is the right shape
for this app. But adopting it means pinning artifact coordinates for its runtime,
its UI, and a separate lifecycle-viewmodel integration whose exact coordinate I
could not verify without a compiler, and every wrong guess costs a full CI
round-trip (D-006). Navigation Compose 2.x, meanwhile, would add route strings
and argument serialization to buy nothing a single-Activity app needs.

Pushing the import screen from a share intent becomes
`backStack.add(ImportPreview(payload))` — no graph declaration, no route
parsing, and type-safe arguments by construction.

**Revisit if:** deep-link handling grows beyond a couple of entry points, or
multi-pane state restoration gets fiddly.

---

## D-011 — Room 3 with the bundled SQLite driver

**2026-08-03 · Accepted**

Persistence uses `androidx.room3` 3.0.1 with `BundledSQLiteDriver`, rather than
Room 2.x with the platform SQLite.

**Considered:** Room 2.7.x — more familiar, no bundled native library.

**Why:** Room 3 is KMP, and the bundled driver runs on a plain JVM. That means
**DAO and migration tests execute as ordinary JVM unit tests** — no emulator, no
Robolectric. Given that CI is the only place Android code compiles at all
(D-006), moving the data layer into cheap, fast, deterministic tests is worth a
lot. The secondary benefit is identical SQLite behaviour on every device
regardless of OS version, which starts to matter once sync exists.

**Costs, accepted:** ~1.5 MB of native SQLite per ABI in the APK, and Room 3
rejects blocking DAO functions — every DAO function must be `suspend` or return
`Flow`. The latter is a discipline, not a limitation.

**Verified 2026-08-04, with one correction.** The claim above was written
without ever being executed. It is true — `Room.inMemoryDatabaseBuilder<T>()`
needs no `Context`, and `app/src/test/.../BoardDaoTest.kt` now runs four real
DAO tests on a plain JVM, no emulator and no Robolectric.

What was missing is that **the test classpath needs the JVM variant of the
native library**, `androidx.sqlite:sqlite-bundled-jvm`. The plain
`sqlite-bundled` dependency resolves to the Android artifact, whose `.so` files
target Android ABIs; a desktop JVM cannot load them and every test dies with:

```
java.lang.UnsatisfiedLinkError: no sqliteJni in java.library.path
```

which then cascades into `NoClassDefFoundError: Could not initialize class
BundledSQLiteDriver$NativeLibraryObject` and reads like a Room problem rather
than a packaging one. One dependency line fixes it. The decision stands; the
justification just needed the footnote it never got.

---

## D-012 — Pin the toolchain explicitly; no Gradle toolchain block

**2026-08-03 · Accepted**

AGP 9.3.0 · Gradle 9.5 (wrapper) · Kotlin 2.4.10 · KSP 2.3.10 · Compose BOM
2026.06.01 · minSdk 26 · targetSdk/compileSdk 37. Java bytecode target 17,
compiled by whatever JDK runs Gradle.

**Considered:** a `jvmToolchain(17) {}` block, which is the conventional way to
pin the JDK.

**Why no toolchain block:** the container has only JDK 21, so a toolchain
request for 17 triggers auto-provisioning — another network fetch, from a host
that may well be blocked like Google's is. Setting `compileOptions` and
`jvmTarget` to 17 instead emits identical bytecode from JDK 21 and removes the
failure mode entirely.

**minSdk 26** gives `java.time` natively — no core library desugaring and no
date-time dependency, on a platform floor that covers effectively every active
device in 2026. **targetSdk 37** clears Google Play's ≥36 requirement that takes
effect 31 Aug 2026.

Versions were checked against live sources rather than recalled, but they cannot
be resolved locally (D-006). If CI rejects one, it's a one-line fix in
`gradle/libs.versions.toml`.

---

## D-013 — Commit a debug signing keystore

**2026-08-03 · Accepted**

A `debug.keystore` is committed to the repo and used by `signingConfigs.debug`,
with the standard non-secret credentials (`androiddebugkey` / `android`).

**Considered:** letting each build generate its own debug key (the default); or
storing one as a base64 repository secret.

**Why:** debug keystores are generated per-machine, so a CI-built APK is signed
with a different key on every run. Installing the next build then fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` and the only fix is uninstalling — **losing
all app data, on every single update.** For an app distributed by sideloading CI
artifacts, that's not a papercut, it's data loss.

A stable committed key fixes it with no configuration on the user's side. Debug
keystores are non-secret by design — the Android SDK ships one with a published
password — so committing it leaks nothing. `.gitignore` still excludes every
other `*.jks` / `*.keystore`, and this file is signing-debug-only.

`versionCode` comes from `GITHUB_RUN_NUMBER`, so successive artifacts are real
upgrades rather than reinstalls.

**This does not apply to release signing.** A release key must never be
committed.

---

## D-014 — Superseded: briefly opted out of the AGP 9 DSL

**2026-08-03 · Superseded by D-015 the same day**

Set `android.newDsl=false` to keep the app module on the pre-AGP-9 Android DSL,
intending to migrate later.

**Why it was wrong:** the flag is itself already deprecated — AGP warns that
`android.newDsl=false` "is deprecated, the current default is true, it will be
removed in version 10.0". Opting out bought a compatibility mode with a shorter
remaining life than the thing it was avoiding, and it did not even fix the
build: with the flag set, the old `android { }` accessor is deprecated at error
level, so the script still failed to compile.

Left here rather than deleted, because "we tried the escape hatch and it was a
dead end" is worth knowing.

---

## D-015 — Use the AGP 9 DSL, and configure Room through KSP

**2026-08-03 · Accepted**

The app module targets the new AGP 9 Android DSL directly, with no compatibility
flag. The Room schema directory is set as a KSP argument rather than through the
Room Gradle plugin, which is no longer applied.

**Considered:** staying on the old DSL behind `android.newDsl=false` (D-014).

**Why:** improving the error output came first, and it paid immediately. With
`--stacktrace` removed, CI reported three specific script-compilation errors
instead of 120 lines of Gradle internals — a deprecated `android { }` accessor
and an unresolved `room { schemaDirectory(...) }`. Guessing would not have found
the second one.

The new DSL is the default and the only one with a future, so going forward
costs the same as going back and doesn't need doing twice.

**Dropping the Room Gradle plugin** is the same trade as D-004 and D-010: it
buys a marginally nicer DSL in exchange for another plugin to resolve, another
type-safe accessor to generate, and another failure mode in a build that cannot
be compiled locally. `ksp { arg("room.schemaLocation", …) }` has done the job
for years and needs none of it.

The `release` build type is also left at its defaults for now. CI only assembles
debug, and minification is another thing to get wrong while the build is being
stabilised; it will be configured before there is ever a release to ship.

---

## D-016 — A fixed dark theme; no Material You

**2026-08-04 · Accepted**

The app ships one colour scheme: the near-black-and-orange design in
`design/ProjectMate.dc.html`. `dynamicColor` is gone, and there is no light
variant.

**Considered:** keeping Material You, which is the Android default and was what
the app actually shipped until now.

**Why:** dynamic colour derives the whole scheme from the device wallpaper. On
the test device that produced a pastel lavender-and-mint app — pleasant, and
completely unrelated to the design. The accent orange is the one thing that
makes ProjectMate recognisable, and Material You replaces exactly that.

The trade is real and accepted: the app no longer matches a user's system
theming. For a single-user tool with a deliberate visual identity, matching the
design wins.

**Board accents are still per-board** — they come from the board's own
`accentColor` and drive the monogram tile. It's the chrome that's fixed, not
the content.

**Costs:** no light theme means the app is dark in a bright room. Adding one
later means defining a second full palette, since the design only specifies
dark. Not free, but not blocked either.

---

## D-017 — The back stack survives configuration changes

**2026-08-04 · Accepted**

`rememberNavigator()` uses `rememberSaveable` with an explicit `Saver` that
encodes every tab's stack, rather than plain `remember`.

**Why:** `remember` is scoped to the composition, which Android throws away on
rotation. Found by rotating a device while looking at a board: the app returned
to the Boards root, silently losing your place. With four independent tab
stacks, that's four places lost at once.

Screens carry at most one string argument, so each encodes as `type:arg` split
on the first colon — ids can contain anything without escaping.

**`ImportPreview` is deliberately not restored** and degrades to the Boards
root. Its argument is an entire JSON payload, and reviving a half-confirmed
import after process death is worse than asking for the file again.

---

## D-018 — The duplicate-Kotlin-plugin warning is unfixable here, and that's accepted

**2026-08-04 · Accepted**

Every build prints:

```
The Kotlin Gradle plugin was loaded multiple times in different subprojects,
which is not supported and may break the build.
... add the Kotlin plugin to the common parent project or the root project,
then remove the versions in the subprojects.
```

**Following that advice breaks the build.** Tried and reverted. Declaring
`org.jetbrains.kotlin.jvm` in the root `plugins {}` block — even `apply false` —
puts the standalone Kotlin Gradle Plugin on the shared buildscript classpath,
where `:app` picks it up next to AGP 9's built-in Kotlin support:

```
Failed to apply plugin 'com.android.internal.application'.
> Could not create an instance of type ...mpp.KotlinAndroidTarget
   > com/android/build/gradle/api/BaseVariant
```

`BaseVariant` is the legacy variant API AGP 9 removed. The external Kotlin plugin
still reaches for it; AGP's built-in one doesn't.

**Why it can't be fixed:** `:core` is a pure JVM module and genuinely needs
`kotlin.jvm`; `:app` gets Kotlin from AGP and must not also see the standalone
plugin. Two different Kotlin plugin loads is the only configuration that works.
This is the same AGP 9 transition that already cost us `kotlin-android`
(see the note in `gradle/libs.versions.toml`).

**Accepted because** the warning has been wrong so far — the build works, both CI
jobs pass, and the app runs on a device. Revisit when the Kotlin Gradle Plugin
catches up with AGP 9's variant API. Until then the warning is noise, and the
comment in the root `build.gradle.kts` says so at the point someone would try.

---

## D-019 — One palette, shared by templates and the colour picker

**2026-08-04 · Accepted**

Status and board colours live in `core/.../template/Palette.kt`, public, and
`BoardTemplates` seeds from it.

**Considered:** leaving the nine colour constants private inside `BoardTemplates`,
as they were.

**Why:** the status editor's colour picker has to offer a set of colours. If that
set and the templates' set were declared separately they would drift, and editing
a status would silently shift its colour to the nearest thing the picker knew
about. Two tests now assert that every seeded colour is one the picker offers.

The values are the design comp's, replacing the earlier blue/green/violet, which
clashed with the black-and-orange chrome (D-016). Statuses are coloured by
**meaning** — not started, queued, in flight, blocked, finished — so a board still
reads correctly after every status has been renamed, which is the point of
categories (D-002).

**Note:** colours are copied into rows at seed time, so **existing boards keep the
old ones**. Only newly created boards pick these up. Recolouring existing rows
would mean overwriting a user's own edits, which is worse than the inconsistency.

---

## D-020 — All-day due dates are normalised to the local day, not UTC midnight

**2026-08-04 · Accepted**

The date picker's answer passes through `DueDates.allDayOn(date, localZone)`
before it is stored, rather than being saved as the picker returns it.

**Why:** Material's `DatePicker` reports the selected day as **midnight UTC**.
Stored raw and read back in a zone behind UTC, that is the previous day. Found
on a device in `America/New_York` (UTC−4): picking 1 August saved and displayed
31 July. It would have looked correct in London and wrong across the Americas.

`DueDates.allDayOn` already existed for exactly this, with a test named "all-day
normalisation lands on the start of the local day". The bug was not writing the
helper — it was not calling it.

**Worth remembering:** this class of bug is invisible to the compiler and to any
test that runs in the same timezone as the developer. It surfaced within a
minute of running the app on real hardware. `DueDatesTest` already covers the
timezone dependency ("which day it is depends on the users timezone"); the gap
was between `:core` and the UI, where nothing was testing.

---

## D-021 — Moving an item carries its board, not just its status

**2026-08-04 · Accepted**

`ItemDao.move` writes `boardId` alongside `statusId`.

**Why:** it previously wrote only `statusId`. A status belongs to exactly one
board, so setting one without the other leaves a row claiming a board whose
columns it is not in — it vanishes from the destination kanban (which filters by
`boardId`), stays in the list it came from, and would render a status name from
somewhere else entirely.

The kanban never exposed this: dragging between columns keeps the same board, so
`boardId` was already correct. Inbox triage was the first thing to move an item
*across* boards, and it failed on the first attempt on a device — the item simply
stayed in the Inbox.

`ItemMoveTest` now covers it: the board follows the status, a triaged item leaves
the Inbox, the Inbox query only returns the system board, and dismissing archives.

**Worth noting** that this is the second bug in a row found by running the app
rather than compiling it, and the second where the type system was no help — both
were about a value being right in the only situation that had been exercised.
