// Intentionally minimal — no `plugins {}` block at all.
//
// Plugins are declared in each module rather than being listed here with
// `apply false`, because a root-level `plugins {}` block resolves every plugin
// artifact at configuration time — including the Android Gradle Plugin, which
// cannot be fetched in the development container. Declaring per-module keeps
// `:core` buildable there.
//
// See docs/DECISIONS.md D-006.
//
// ---
//
// **Every build prints this warning. Do not act on it:**
//
//     The Kotlin Gradle plugin was loaded multiple times in different
//     subprojects... Please add the Kotlin plugin to the common parent project
//     or the root project, then remove the versions in the subprojects.
//
// Following that advice breaks the build. Declaring `org.jetbrains.kotlin.jvm`
// here — even with `apply false` — puts the standalone Kotlin Gradle Plugin on
// the shared buildscript classpath, where `:app` picks it up alongside AGP 9's
// *built-in* Kotlin support. The two collide immediately:
//
//     Failed to apply plugin 'com.android.internal.application'.
//     > Could not create an instance of type ...mpp.KotlinAndroidTarget
//        > com/android/build/gradle/api/BaseVariant
//
// `BaseVariant` is the legacy variant API that AGP 9 removed. The external
// Kotlin plugin still reaches for it; AGP's built-in one does not.
//
// The duplicate load is therefore unavoidable in this shape: `:core` genuinely
// needs `kotlin.jvm` (it is a pure JVM module) and `:app` gets Kotlin from AGP.
// Tried and reverted 2026-08-04. See docs/DECISIONS.md D-018.

tasks.register("cleanAll", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
