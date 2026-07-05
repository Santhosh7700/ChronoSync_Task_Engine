package com.example.taskflow.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_table",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey
    val id: Int = 1, // Single-user app, hardcoded to 1

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "security_pin")
    val securityPin: String
)
