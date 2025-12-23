package com.adrianosilva.githubexplorer.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.adrianosilva.githubexplorer.data.deconstructToEntities
import com.adrianosilva.githubexplorer.data.local.dao.GithubRemotePageDao
import com.adrianosilva.githubexplorer.data.local.dao.GithubRepositoryDao
import com.adrianosilva.githubexplorer.data.local.dao.GithubUserDao
import com.adrianosilva.githubexplorer.data.local.entity.GithubRemotePageEntity
import com.adrianosilva.githubexplorer.data.mapToDomain
import com.adrianosilva.githubexplorer.data.remote.GithubApiService
import com.adrianosilva.githubexplorer.data.remote.GithubPagingRemoteMediator
import com.adrianosilva.githubexplorer.domain.model.Repository
import com.adrianosilva.githubexplorer.domain.repository.RepositoriesProvider
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GithubRepositoryImpl(
    private val apiService: GithubApiService,
    private val githubRepositoryDao: GithubRepositoryDao,
    private val githubUserDao: GithubUserDao,
    private val githubRemotePageDao: GithubRemotePageDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): RepositoriesProvider {

    override fun observeRepositories(): Flow<List<Repository>> {
        return githubRepositoryDao.observeAllRepositories()
            .map { entities ->
                entities.map { it.mapToDomain() }
            }
    }

    override suspend fun getRepositoryById(id: Int): Repository? {
        return withContext(dispatcher) {
            val entity = githubRepositoryDao.getRepositoryById(id)
            entity?.mapToDomain()
        }
    }

    override fun observeRepositoriesByLanguage(language: String): Flow<List<Repository>> {
        return githubRepositoryDao.observeAllRepositoriesByLanguage(language)
            .map { entities ->
                entities.map { it.mapToDomain() }
            }
    }

    override fun observeRepositoryById(id: Int): Flow<Repository> {
        return githubRepositoryDao.observeRepositoryById(id)
            .map { it.mapToDomain() }
    }

    override suspend fun refreshRepositories(): Result<Unit> = withContext(dispatcher) {
        val remoteResult = apiService.getPublicRepositories()

        if (remoteResult is Result.Error) {
            return@withContext Result.Error(remoteResult.reason)
        } else {
            val successResult = (remoteResult as Result.Success)
            val (repoEntities, userEntities) = successResult.data.map { it.deconstructToEntities() }.unzip()

            launch {
                githubUserDao.upsertAll(userEntities)
            }

            githubRepositoryDao.upsertAll(repoEntities)
            Timber.w("Refreshed and stored initial ${repoEntities.size} repositories.")
            return@withContext Result.Success(Unit)
        }
    }

    override suspend fun fetchNextPageRepositories(): Result<Unit> = withContext(dispatcher) {
        val lastRepoId = githubRepositoryDao.getLastRepositoryId() ?: 0
        Timber.w("Fetching next page of repositories since ID: $lastRepoId")
        val nextReposResult = apiService.getPublicRepositories(since = lastRepoId)

        if (nextReposResult is Result.Error) {
            return@withContext Result.Error(nextReposResult.reason)
        } else {
            val successResult = (nextReposResult as Result.Success)
            val (repoEntities, userEntities) = successResult.data.map { it.deconstructToEntities() }.unzip()

            launch {
                githubUserDao.upsertAll(userEntities)
            }

            githubRepositoryDao.upsertAll(repoEntities)
            Timber.w("Fetched and stored ${repoEntities.size} repositories.")
            return@withContext Result.Success(Unit)
        }
    }

    override suspend fun fetchRepositoryDetails(repositoryId: Int): Result<Unit> = withContext(dispatcher) {
        val repoEntity = githubRepositoryDao.getRepositoryById(repositoryId)

        if (repoEntity == null) {
            Timber.e("Repository with ID $repositoryId not found in local database.")
            return@withContext Result.Error(ErrorReason.NoData)
        }

        val fullName = repoEntity.fullName
        val detailsResult = apiService.getRepositoryDetails(fullName)

        if (detailsResult is Result.Error) {
            return@withContext Result.Error(detailsResult.reason)
        } else {
            val detailedRepoDto = (detailsResult as Result.Success).data
            val (updatedRepoEntity, updatedUserEntity) = detailedRepoDto.deconstructToEntities()

            launch {
                githubUserDao.upsert(updatedUserEntity)
            }

            githubRepositoryDao.upsert(updatedRepoEntity)
            Timber.w("Fetched and stored details for repository ID $repositoryId.")
            return@withContext Result.Success(Unit)
        }
    }

    override suspend fun fetchRepositoriesByLanguage(language: String): Result<Unit> = withContext(dispatcher) {
        if (language.isBlank()) {
            return@withContext Result.Error(ErrorReason.NoData)
        }

        val searchQuery = "language:$language"
        val page = githubRemotePageDao.getByQuery(searchQuery)?.nextPage ?: 1

        if (page == 11) {
            Timber.w("Reached GitHub API search limit for language: $language. No further pages will be fetched.")
            return@withContext Result.Success(Unit)
        }

        val searchResult = apiService.searchRepositories(query = searchQuery, page = page)


        if (searchResult is Result.Error) {
            return@withContext Result.Error(searchResult.reason)
        } else {
            val successResult = (searchResult as Result.Success)
            val (repoEntities, userEntities) = successResult.data.items.map { it.deconstructToEntities() }.unzip()

            launch {
                githubUserDao.upsertAll(userEntities)
            }

            launch {
                // Update page number for query
                githubRemotePageDao.upsert(
                    GithubRemotePageEntity(
                        query = searchQuery,
                        nextPage = page + 1
                    )
                )
            }

            githubRepositoryDao.upsertAll(repoEntities)
            Timber.w("Fetched and stored ${repoEntities.size} repositories for language: $language. page: $page")
            return@withContext Result.Success(Unit)
        }

    }

    override suspend fun isRepositoryListEmpty(): Boolean {
        return withContext(dispatcher) {
            val count = githubRepositoryDao.getRepositoryCount()
            return@withContext count == 0
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun observePaginatedRepositories(language: String): Flow<PagingData<Repository>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
            ),
            remoteMediator = GithubPagingRemoteMediator(
                apiService = apiService,
                repositoryDao = githubRepositoryDao,
                userDao = githubUserDao,
                pageDao = githubRemotePageDao,
                query = language
            ),
            pagingSourceFactory = {
                if (language.isBlank()) {
                    githubRepositoryDao.pagingSourceRepositories()
                } else {
                    githubRepositoryDao.pagingRepositoriesByLanguage(language)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.mapToDomain() }
        }
    }
}