package com.adrianosilva.githubexplorer.data.local

import androidx.paging.PagingData
import com.adrianosilva.githubexplorer.domain.model.Repository
import com.adrianosilva.githubexplorer.domain.repository.RepositoriesProvider
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class FakeRepositoriesProvider: RepositoriesProvider {
    val repositoriesFlow = MutableStateFlow<List<Repository>>(emptyList())

    var shouldReturnNetworkError: Boolean = false
    var refreshCallCount: Int = 0
    var fetchMoreCallCount: Int = 0
    var fetchSearchCount: Int = 0
    var fetchDetailsCount: Int = 0

    override fun observeRepositories(): Flow<List<Repository>> {
        return repositoriesFlow
    }

    override suspend fun getRepositoryById(id: Int): Repository? {
        return repositoriesFlow.value.find { it.id == id }
    }

    override fun observeRepositoriesByLanguage(language: String): Flow<List<Repository>> {
        return repositoriesFlow.map { repos ->
            repos.filter { it.language.equals(language, ignoreCase = true) }
        }
    }

    override fun observeRepositoryById(id: Int): Flow<Repository> {
        return repositoriesFlow.map { repos ->
            repos.find { it.id == id }
        }.filterNotNull()
    }

    override suspend fun refreshRepositories(): Result<Unit> {
        refreshCallCount++
        print("refreshRepositories called $refreshCallCount times")
        return if (shouldReturnNetworkError) {
            Result.Error(ErrorReason.NetworkError("Simulated network error"))
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun fetchNextPageRepositories(): Result<Unit> {
        fetchMoreCallCount++
        return if (shouldReturnNetworkError) {
            Result.Error(ErrorReason.NetworkError("Simulated network error"))
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun fetchRepositoryDetails(repositoryId: Int): Result<Unit> {
        fetchDetailsCount++
        return if (shouldReturnNetworkError) {
            Result.Error(ErrorReason.NetworkError("Simulated network error"))
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun fetchRepositoriesByLanguage(language: String): Result<Unit> {
        fetchSearchCount++
        return if (shouldReturnNetworkError) {
            Result.Error(ErrorReason.NetworkError("Simulated network error"))
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun isRepositoryListEmpty(): Boolean {
        return repositoriesFlow.value.isEmpty()
    }

    override fun observePaginatedRepositories(language: String): Flow<PagingData<Repository>> {
        throw NotImplementedError()
    }
}