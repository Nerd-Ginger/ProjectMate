pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProjectMate"

// :core is pure Kotlin/JVM and resolves entirely from Maven Central, so it
// builds and tests anywhere.
include(":core")

// :app needs the Android SDK and Google's Maven repository. The development
// container has neither — dl.google.com is blocked by network policy — so
// including :app unconditionally would make even `:core:test` fail during
// configuration.
//
// Include it only where it can actually be built. CI sets ANDROID_HOME (via
// android-actions/setup-android), and Android Studio writes local.properties,
// so both of those work without anyone thinking about it.
//
// See docs/DECISIONS.md D-006.
val androidSdkAvailable =
    !System.getenv("ANDROID_HOME").isNullOrBlank() ||
        !System.getenv("ANDROID_SDK_ROOT").isNullOrBlank() ||
        file("local.properties").exists() ||
        System.getenv("PROJECTMATE_FORCE_ANDROID") == "true"

if (androidSdkAvailable) {
    include(":app")
} else {
    logger.lifecycle(
        "No Android SDK detected — skipping :app. Only :core will be configured. " +
            "Set ANDROID_HOME (or create local.properties) to build the app.",
    )
}
