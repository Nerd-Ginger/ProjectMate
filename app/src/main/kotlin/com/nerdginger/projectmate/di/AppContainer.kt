package com.nerdginger.projectmate.di

import android.content.Context
import com.nerdginger.projectmate.data.ProjectMateDatabase
import com.nerdginger.projectmate.data.dao.AppMetaDao

/**
 * The dependency graph, wired by hand.
 *
 * Not Hilt — see docs/DECISIONS.md D-004. Everything is lazy so nothing touches
 * disk during `Application.onCreate`.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: ProjectMateDatabase by lazy { ProjectMateDatabase.build(appContext) }

    val appMetaDao: AppMetaDao by lazy { database.appMetaDao() }
}
