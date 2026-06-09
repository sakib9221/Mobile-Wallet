package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Immutable
@Entity(tableName = "bajar_items", indices = [androidx.room.Index("userId")])
data class BajarItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String,
    val isCompleted: Boolean = false,
    val userId: String = "local_user"
) : Serializable
