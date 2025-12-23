package com.adrianosilva.githubexplorer.domain.repository

import androidx.paging.PagingData
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

    /**
     * NOTE: Exposing PagingData in the domain layer is a pragmatic architectural trade-off.
     * This approach reduces boilerplate.
     */
    fun observePaginatedRepositories(language: String): Flow<PagingData<Repository>>
}