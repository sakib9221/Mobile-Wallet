package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Immutable
@Entity(tableName = "debt_records", indices = [androidx.room.Index("userId")])
data class DebtRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Double,
    val timestamp: Long,
    val direction: String, // "PAYABLE" (someone gets money from me) or "RECEIVABLE" (someone owes me money)
    val isSettled: Boolean = false,
    val note: String = "",
    val userId: String = "local_user"
) : Serializable
