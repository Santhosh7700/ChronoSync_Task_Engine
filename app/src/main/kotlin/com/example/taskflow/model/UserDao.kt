package com.example.taskflow.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("SELECT * FROM user_table WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): User?

    @Query("SELECT * FROM user_table WHERE username = :username LIMIT 1")
    suspend fun queryUserByUsername(username: String): User?

    @Query("DELETE FROM user_table")
    suspend fun clearUserProfile()
}
