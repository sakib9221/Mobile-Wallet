package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtRecordDao {
    @Query("SELECT * FROM debt_records WHERE userId = :userId ORDER BY timestamp DESC, id DESC")
    fun getAllDebts(userId: String): Flow<List<DebtRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtRecord)

    @Update
    suspend fun updateDebt(debt: DebtRecord)

    @Delete
    suspend fun deleteDebt(debt: DebtRecord)

    @Query("DELETE FROM debt_records WHERE userId = :userId")
    suspend fun clearDebtsForUser(userId: String)
}
