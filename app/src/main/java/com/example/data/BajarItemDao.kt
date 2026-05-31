package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BajarItemDao {
    @Query("SELECT * FROM bajar_items WHERE userId = :userId ORDER BY id DESC")
    fun getAllBajarItems(userId: String): Flow<List<BajarItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBajarItem(item: BajarItem)

    @Update
    suspend fun updateBajarItem(item: BajarItem)

    @Delete
    suspend fun deleteBajarItem(item: BajarItem)

    @Query("DELETE FROM bajar_items WHERE userId = :userId")
    suspend fun clearBajarItemsForUser(userId: String)

    @Query("DELETE FROM bajar_items WHERE userId = :userId AND isCompleted = 1")
    suspend fun deleteCompletedBajarItems(userId: String)
}
