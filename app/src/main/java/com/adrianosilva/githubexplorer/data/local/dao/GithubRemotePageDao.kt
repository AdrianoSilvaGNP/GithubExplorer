package com.adrianosilva.githubexplorer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.adrianosilva.githubexplorer.data.local.entity.GithubRemotePageEntity

@Dao
interface GithubRemotePageDao {

    @Upsert
    suspend fun upsert(page: GithubRemotePageEntity)

    @Query("SELECT * FROM remote_pages WHERE `query` = :query COLLATE NOCASE LIMIT 1")
    suspend fun getByQuery(query: String): GithubRemotePageEntity?

    @Query("DELETE FROM remote_pages WHERE `query` = :query")
    suspend fun deleteByQuery(query: String)

}