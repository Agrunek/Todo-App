package com.app.todo.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val creation: Long,
    val expiration: Long,
    val finished: Boolean,
    val notify: Boolean,
    val category: String,
    val attachment: String? = null
)