package com.adrianosilva.githubexplorer.data.local

import androidx.paging.PagingSource
import com.adrianosilva.githubexplorer.data.local.dao.GithubRepositoryDao
import com.adrianosilva.githubexplorer.data.local.entity.GithubRepositoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeGithubRepositoryDao: GithubRepositoryDao {
    private val repositoriesFlow = MutableStateFlow<List<GithubRepositoryEntity>>(emptyList())

    override suspend fun upsert(repository: GithubRepositoryEntity) {
        val currentList = repositoriesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == repository.id }
        if (index >= 0) {
            currentList[index] = repository
        } else {
            currentList.add(repository)
        }
        repositoriesFlow.update { currentList }
    }

    override suspend fun upsertAll(repositories: List<GithubRepositoryEntity>) {
        println("FakeGithubRepositoryDao: upsertAll called with ${repositories.size} repositories")
        val currentList = repositoriesFlow.value.toMutableList()
        for (repository in repositories) {
            val index = currentList.indexOfFirst { it.id == repository.id }
            if (index >= 0) {
                currentList[index] = repository
            } else {
                currentList.add(repository)
            }
        }
        println("FakeGithubRepositoryDao: current list size after upsertAll is ${currentList.size}")
        repositoriesFlow.update { currentList }
    }

    override suspend fun getAllRepositories(): List<GithubRepositoryEntity> {
        return repositoriesFlow.value
    }

    override fun observeAllRepositoriesByLanguage(language: String): Flow<List<GithubRepositoryEntity>> {
        return repositoriesFlow.map { list ->
            list.filter { it.language?.contains(language, ignoreCase = true) == true }
        }
    }

    override fun observeAllRepositories(): Flow<List<GithubRepositoryEntity>> {
        return repositoriesFlow.asStateFlow()
    }

    override suspend fun getLastRepositoryId(): Int? {
        return repositoriesFlow.value.maxByOrNull { it.id }?.id
    }

    override suspend fun getRepositoryById(id: Int): GithubRepositoryEntity? {
        return repositoriesFlow.value.firstOrNull { it.id == id }
    }

    override fun observeRepositoryById(id: Int): Flow<GithubRepositoryEntity> {
        return repositoriesFlow.map { list ->
            list.first { it.id == id }
        }
    }

    override suspend fun getRepositoryCount(): Int {
        return repositoriesFlow.value.size
    }

    override fun pagingSourceRepositories(): PagingSource<Int, GithubRepositoryEntity> {
        throw NotImplementedError()
    }

    override fun pagingRepositoriesByLanguage(language: String): PagingSource<Int, GithubRepositoryEntity> {
        throw NotImplementedError()
    }
}