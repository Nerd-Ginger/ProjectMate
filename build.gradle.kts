// Intentionally minimal.
//
// Plugins are declared in each module rather than being listed here with
// `apply false`, because a root-level `plugins {}` block resolves every plugin
// artifact at configuration time — including the Android Gradle Plugin, which
// cannot be fetched in the development container. Declaring per-module keeps
// `:core` buildable there.
//
// See docs/DECISIONS.md D-006.

tasks.register("cleanAll", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
