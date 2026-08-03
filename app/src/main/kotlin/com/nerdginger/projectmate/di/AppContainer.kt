package com.nerdginger.projectmate.di

import android.content.Context
import com.nerdginger.projectmate.data.DatabaseSeeder
import com.nerdginger.projectmate.data.ProjectMateDatabase
import com.nerdginger.projectmate.data.repository.BoardRepository

/**
 * The dependency graph, wired by hand.
 *
 * Not Hilt — see docs/DECISIONS.md D-004. Everything is lazy so nothing touches
 * disk during `Application.onCreate`.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: ProjectMateDatabase by lazy { ProjectMateDatabase.build(appContext) }

    val boardDao by lazy { database.boardDao() }
    val statusDao by lazy { database.statusDao() }
    val itemDao by lazy { database.itemDao() }
    val checklistDao by lazy { database.checklistDao() }
    val tagDao by lazy { database.tagDao() }
    val itemLinkDao by lazy { database.itemLinkDao() }
    val featureRequestMetaDao by lazy { database.featureRequestMetaDao() }
    val savedViewDao by lazy { database.savedViewDao() }
    val appMetaDao by lazy { database.appMetaDao() }

    val seeder: DatabaseSeeder by lazy {
        DatabaseSeeder(boardDao, statusDao, appMetaDao)
    }

    val boardRepository: BoardRepository by lazy {
        BoardRepository(boardDao, statusDao, seeder)
    }
}
