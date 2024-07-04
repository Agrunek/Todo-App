package com.app.todo.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Insert
    fun insertAll(vararg tasks: Task)

    @Update
    fun updateTasks(vararg tasks: Task)

    @Delete
    fun delete(task: Task)

    @Query("SELECT * FROM task")
    fun getAll(): List<Task>
}