package com.autasktic.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val date: String,
    val time: String?,
    val colour: String,
    val isCompleted: Boolean = false,
    val createdAt: String,
    val completedAt: String? = null,
)
