package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateLong DESC, id DESC")
    fun getAllTransactions(userId: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearTransactionsForUser(userId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
