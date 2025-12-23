package com.adrianosilva.githubexplorer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.adrianosilva.githubexplorer.data.local.entity.GithubUserEntity

@Dao
interface GithubUserDao {

    @Upsert
    suspend fun upsert(user: GithubUserEntity)

    @Upsert
    suspend fun upsertAll(users: List<GithubUserEntity>)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<GithubUserEntity>

}