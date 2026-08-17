package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdBajarDao {
    @Query("SELECT * FROM household_bajar_records WHERE userId = :userId ORDER BY dateLong DESC, id DESC")
    fun getAllHouseholdBajar(userId: String): Flow<List<HouseholdBajarRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouseholdBajar(record: HouseholdBajarRecord): Long

    @Update
    suspend fun updateHouseholdBajar(record: HouseholdBajarRecord)

    @Delete
    suspend fun deleteHouseholdBajar(record: HouseholdBajarRecord)

    @Query("DELETE FROM household_bajar_records WHERE userId = :userId")
    suspend fun clearHouseholdBajarForUser(userId: String)
}
