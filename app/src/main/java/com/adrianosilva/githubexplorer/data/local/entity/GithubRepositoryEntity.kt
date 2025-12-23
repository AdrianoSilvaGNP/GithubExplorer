package com.adrianosilva.githubexplorer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repositories",
    indices = [
        Index(value = ["id"], unique = true)
    ]
)
data class GithubRepositoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val fullName: String,
    val description: String?,
    val ownerAvatarUrl: String,
    val htmlUrl: String,
    val stargazersCount: Int?,
    val forksCount: Int?,
    val openIssuesCount: Int?,
    val lastUpdated: String?,
    val language: String?,
    val license: String?
)
