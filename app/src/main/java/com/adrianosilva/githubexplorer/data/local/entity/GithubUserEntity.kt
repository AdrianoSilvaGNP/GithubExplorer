package com.adrianosilva.githubexplorer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["id"], unique = true)
    ]
)
data class GithubUserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val avatarUrl: String
)