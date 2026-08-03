package com.nerdginger.projectmate.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nerdginger.projectmate.data.dao.AppMetaDao
import com.nerdginger.projectmate.data.entity.AppMetaEntity
import kotlinx.coroutines.Dispatchers

/**
 * The app's database.
 *
 * Currently one table. The rest of the schema in docs/DATA_MODEL.md lands next
 * — this slice exists first to prove the Room 3 toolchain end to end (artifact
 * coordinates, package names, KSP codegen, schema export) in a build that can
 * only be compiled in CI. See docs/DECISIONS.md D-006.
 *
 * Schemas are exported to `app/schemas/` and committed, so migrations can be
 * tested against real historical versions.
 */
@Database(
    entities = [AppMetaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ProjectMateDatabase : RoomDatabase() {

    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val NAME: String = "projectmate.db"

        fun build(context: Context): ProjectMateDatabase =
            Room.databaseBuilder<ProjectMateDatabase>(
                context = context.applicationContext,
                name = context.applicationContext.getDatabasePath(NAME).absolutePath,
            )
                // Bundled SQLite rather than the platform's: identical
                // behaviour on every device regardless of OS version, which
                // starts to matter once sync exists. See D-011.
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
