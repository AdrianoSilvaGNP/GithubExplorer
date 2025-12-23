package com.adrianosilva.githubexplorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "remote_pages",
)
data class GithubRemotePageEntity(
    @PrimaryKey val query: String,
    val nextPage: Int?,
)