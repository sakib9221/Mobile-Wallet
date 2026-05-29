package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
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
}
