package com.adrianosilva.githubexplorer.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.adrianosilva.githubexplorer.data.local.entity.GithubRepositoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GithubRepositoryDao {

    @Upsert
    suspend fun upsert(repository: GithubRepositoryEntity)

    @Upsert
    suspend fun upsertAll(repositories: List<GithubRepositoryEntity>)

    @Query("SELECT * FROM repositories ORDER BY id ASC")
    suspend fun getAllRepositories(): List<GithubRepositoryEntity>

    @Query("SELECT * FROM repositories WHERE language LIKE :language || '%' COLLATE NOCASE ORDER BY id ASC")
    fun observeAllRepositoriesByLanguage(language: String): Flow<List<GithubRepositoryEntity>>

    @Query("SELECT * FROM repositories ORDER BY id ASC")
    fun observeAllRepositories(): Flow<List<GithubRepositoryEntity>>

    @Query("SELECT id FROM repositories ORDER BY id DESC LIMIT 1")
    suspend fun getLastRepositoryId(): Int?

    @Query("SELECT * FROM repositories WHERE id = :id LIMIT 1")
    suspend fun getRepositoryById(id: Int): GithubRepositoryEntity?

    @Query("SELECT * FROM repositories WHERE id = :id LIMIT 1")
    fun observeRepositoryById(id: Int): Flow<GithubRepositoryEntity>

    @Query("SELECT COUNT(*) FROM repositories")
    suspend fun getRepositoryCount(): Int

    @Query("SELECT * FROM repositories ORDER BY id ASC")
    fun pagingSourceRepositories(): PagingSource<Int, GithubRepositoryEntity>

    @Query("SELECT * FROM repositories WHERE language LIKE :language || '%' COLLATE NOCASE ORDER BY id ASC")
    fun pagingRepositoriesByLanguage(language: String): PagingSource<Int, GithubRepositoryEntity>

}