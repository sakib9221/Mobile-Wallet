package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val type: String, // "INCOME" or "EXPENSE"
    val dateLong: Long, // LocalDate timestamp in milliseconds
    val note: String,
    val userId: String = "local_user"
) : Serializable
