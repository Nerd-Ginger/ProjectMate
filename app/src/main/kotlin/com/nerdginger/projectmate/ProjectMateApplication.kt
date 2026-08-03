package com.nerdginger.projectmate

import android.app.Application
import com.nerdginger.projectmate.di.AppContainer

/**
 * Owns the dependency graph for the process.
 *
 * Wiring is done by hand rather than with Hilt — see docs/DECISIONS.md D-004.
 */
class ProjectMateApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Cheap: everything inside is lazy, so nothing touches disk here.
        container = AppContainer(this)
    }
}
