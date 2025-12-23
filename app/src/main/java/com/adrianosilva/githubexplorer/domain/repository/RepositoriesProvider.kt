package com.adrianosilva.githubexplorer.domain.repository

import com.adrianosilva.githubexplorer.domain.model.Repository
import com.adrianosilva.githubexplorer.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Provides access the repositories data.
 * This is a repository pattern. But calling it "RepositoriesRepository" sounds weird.
 * Therefore, "Provider".
 */
interface RepositoriesProvider {

    fun observeRepositories(): Flow<List<Repository>>

    suspend fun getRepositoryById(id: Int): Repository?

    fun observeRepositoriesByLanguage(language: String): Flow<List<Repository>>

    fun observeRepositoryById(id: Int): Flow<Repository>

    suspend fun refreshRepositories(): Result<Unit>

    suspend fun fetchNextPageRepositories(): Result<Unit>

    suspend fun fetchRepositoryDetails(repositoryId: Int): Result<Unit>

    suspend fun fetchRepositoriesByLanguage(language: String): Result<Unit>

    suspend fun isRepositoryListEmpty(): Boolean
}