package com.nerdginger.projectmate.core

/**
 * Identity of the `:core` module.
 *
 * `:core` is pure Kotlin/JVM by design — it must never gain an Android
 * dependency. See docs/ARCHITECTURE.md.
 */
object BuildInfo {
    const val SCHEMA_VERSION: Int = 1
}
