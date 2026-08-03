package com.nerdginger.projectmate.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.nerdginger.projectmate.data.entity.AppMetaEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room 3 rejects blocking DAO functions at compile time — every function must
 * be `suspend` or return [Flow]. See docs/ARCHITECTURE.md.
 */
@Dao
interface AppMetaDao {

    @Query("SELECT * FROM app_meta WHERE id = :id")
    fun observe(id: Int = AppMetaEntity.SINGLETON_ID): Flow<AppMetaEntity?>

    @Query("SELECT * FROM app_meta WHERE id = :id")
    suspend fun get(id: Int = AppMetaEntity.SINGLETON_ID): AppMetaEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(meta: AppMetaEntity)

    @Upsert
    suspend fun upsert(meta: AppMetaEntity)
}
