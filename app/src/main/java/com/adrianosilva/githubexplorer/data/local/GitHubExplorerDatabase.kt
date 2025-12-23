package com.adrianosilva.githubexplorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.adrianosilva.githubexplorer.data.local.dao.GithubRemotePageDao
import com.adrianosilva.githubexplorer.data.local.dao.GithubRepositoryDao
import com.adrianosilva.githubexplorer.data.local.dao.GithubUserDao
import com.adrianosilva.githubexplorer.data.local.entity.GithubRemotePageEntity
import com.adrianosilva.githubexplorer.data.local.entity.GithubRepositoryEntity
import com.adrianosilva.githubexplorer.data.local.entity.GithubUserEntity

@Database(
    entities = [
        GithubRepositoryEntity::class,
        GithubUserEntity::class,
        GithubRemotePageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GitHubExplorerDatabase: RoomDatabase() {
    abstract fun githubRepositoryDao(): GithubRepositoryDao

    abstract fun githubUserDao(): GithubUserDao

    abstract fun githubRemotePageDao(): GithubRemotePageDao
}