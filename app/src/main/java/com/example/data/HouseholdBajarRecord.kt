package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Immutable
@Entity(
    tableName = "household_bajar_records",
    indices = [Index("userId"), Index("dateLong")]
)
data class HouseholdBajarRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val quantity: String = "1",
    val buyerName: String,
    val cost: Double,
    val dateLong: Long = System.currentTimeMillis(),
    val note: String = "",
    val userId: String = "local_guest"
) : Serializable
