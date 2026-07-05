package com.example.taskflow.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, User::class],
    version = 2, // Bumped version due to schema change
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "taskflow_database"
                )
                    .fallbackToDestructiveMigration() // Useful for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
