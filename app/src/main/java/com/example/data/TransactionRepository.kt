package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val bajarItemDao: BajarItemDao
) {
    fun getAllTransactions(userId: String): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions(userId)

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun clearAllForUser(userId: String) {
        transactionDao.clearTransactionsForUser(userId)
    }

    suspend fun deleteAll() {
        transactionDao.deleteAllTransactions()
    }

    // Bajar List
    fun getAllBajarItems(userId: String): Flow<List<BajarItem>> =
        bajarItemDao.getAllBajarItems(userId)

    suspend fun insertBajarItem(item: BajarItem) {
        bajarItemDao.insertBajarItem(item)
    }

    suspend fun updateBajarItem(item: BajarItem) {
        bajarItemDao.updateBajarItem(item)
    }

    suspend fun deleteBajarItem(item: BajarItem) {
        bajarItemDao.deleteBajarItem(item)
    }

    suspend fun clearBajarItemsForUser(userId: String) {
        bajarItemDao.clearBajarItemsForUser(userId)
    }

    suspend fun deleteCompletedBajarItems(userId: String) {
        bajarItemDao.deleteCompletedBajarItems(userId)
    }
}
