package com.nerdginger.projectmate.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nerdginger.projectmate.data.dao.AppMetaDao
import com.nerdginger.projectmate.data.dao.BoardDao
import com.nerdginger.projectmate.data.dao.ChecklistDao
import com.nerdginger.projectmate.data.dao.FeatureRequestMetaDao
import com.nerdginger.projectmate.data.dao.ItemDao
import com.nerdginger.projectmate.data.dao.ItemLinkDao
import com.nerdginger.projectmate.data.dao.SavedViewDao
import com.nerdginger.projectmate.data.dao.StatusDao
import com.nerdginger.projectmate.data.dao.TagDao
import com.nerdginger.projectmate.data.entity.AppMetaEntity
import com.nerdginger.projectmate.data.entity.BoardEntity
import com.nerdginger.projectmate.data.entity.ChecklistEntryEntity
import com.nerdginger.projectmate.data.entity.FeatureRequestMetaEntity
import com.nerdginger.projectmate.data.entity.ItemEntity
import com.nerdginger.projectmate.data.entity.ItemLinkEntity
import com.nerdginger.projectmate.data.entity.ItemTagCrossRef
import com.nerdginger.projectmate.data.entity.SavedViewEntity
import com.nerdginger.projectmate.data.entity.StatusEntity
import com.nerdginger.projectmate.data.entity.SyncOutboxEntity
import com.nerdginger.projectmate.data.entity.TagEntity
import kotlinx.coroutines.Dispatchers

/**
 * The app's database. Schema is specified in docs/DATA_MODEL.md.
 *
 * No type converters: enum-valued columns store the stable `id` of the
 * corresponding `:core` enum, mapped explicitly at the repository boundary
 * rather than implicitly by Room. Ordinals would silently reinterpret existing
 * rows the moment a constant is reordered.
 *
 * Schemas are exported to `app/schemas/` and committed, so migrations can be
 * tested against real historical versions.
 */
@Database(
    entities = [
        BoardEntity::class,
        StatusEntity::class,
        ItemEntity::class,
        ChecklistEntryEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        ItemLinkEntity::class,
        FeatureRequestMetaEntity::class,
        SavedViewEntity::class,
        SyncOutboxEntity::class,
        AppMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ProjectMateDatabase : RoomDatabase() {

    abstract fun boardDao(): BoardDao

    abstract fun statusDao(): StatusDao

    abstract fun itemDao(): ItemDao

    abstract fun checklistDao(): ChecklistDao

    abstract fun tagDao(): TagDao

    abstract fun itemLinkDao(): ItemLinkDao

    abstract fun featureRequestMetaDao(): FeatureRequestMetaDao

    abstract fun savedViewDao(): SavedViewDao

    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val NAME: String = "projectmate.db"

        fun build(context: Context): ProjectMateDatabase =
            Room.databaseBuilder<ProjectMateDatabase>(
                context = context.applicationContext,
                name = context.applicationContext.getDatabasePath(NAME).absolutePath,
            )
                // Bundled SQLite rather than the platform's: identical behaviour
                // on every device regardless of OS version, which starts to
                // matter once sync exists. See docs/DECISIONS.md D-011.
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
