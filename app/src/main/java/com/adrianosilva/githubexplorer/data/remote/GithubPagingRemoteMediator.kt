@file:OptIn(ExperimentalPagingApi::class)

package com.adrianosilva.githubexplorer.data.remote

import android.net.http.HttpException
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.adrianosilva.githubexplorer.data.deconstructToEntities
import com.adrianosilva.githubexplorer.data.local.dao.GithubRemotePageDao
import com.adrianosilva.githubexplorer.data.local.dao.GithubRepositoryDao
import com.adrianosilva.githubexplorer.data.local.dao.GithubUserDao
import com.adrianosilva.githubexplorer.data.local.entity.GithubRemotePageEntity
import com.adrianosilva.githubexplorer.data.local.entity.GithubRepositoryEntity
import com.adrianosilva.githubexplorer.data.remote.dto.GitHubRepositoryDto
import com.adrianosilva.githubexplorer.data.remote.dto.GithubSearchResponseDto
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import timber.log.Timber

class GithubPagingRemoteMediator(
    private val apiService: GithubApiService,
    private val repositoryDao: GithubRepositoryDao,
    private val userDao: GithubUserDao,
    private val pageDao: GithubRemotePageDao,
    private val query: String,
): RemoteMediator<Int, GithubRepositoryEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GithubRepositoryEntity>
    ): MediatorResult {

        return try {
            val loadKey = when (loadType) {
                LoadType.REFRESH -> {
                    if (query.isBlank()) 0 else 1
                }

                LoadType.PREPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)

                LoadType.APPEND -> {
                    if (query.isBlank()) {
                        val lastItemId = repositoryDao.getLastRepositoryId()
                            ?: return MediatorResult.Success(endOfPaginationReached = true)
                        Timber.d("Last item ID for APPEND: $lastItemId")

                        lastItemId
                    } else {
                        val remoteKey = pageDao.getByQuery(query)
                        remoteKey?.nextPage ?: 1
                    }
                }
            }

            val response = if (query.isBlank()) {
                apiService.getPublicRepositories(loadKey)
            } else {
                apiService.searchRepositories(
                    query = query,
                    page = loadKey
                )
            }

            if (response is Result.Error) {
                when (response.reason) {
                    is ErrorReason.NetworkError ->
                        throw IOException("Network error: ${response.reason.message}")

                    is ErrorReason.Unknown ->
                        throw response.reason.exception

                    else -> throw IOException("Unknown error occurred")
                }
            }

            val successResult = (response as Result.Success)
            val (repoEntities, userEntities) = if (query.isBlank()) {
                @Suppress("UNCHECKED_CAST")
                val data = successResult.data as List<GitHubRepositoryDto>
                data.map { it.deconstructToEntities() }.unzip()
            } else {
                val data = successResult.data as GithubSearchResponseDto
                data.items.map { it.deconstructToEntities() }.unzip()
            }


            userDao.upsertAll(userEntities)
            repositoryDao.upsertAll(repoEntities)

            if (query.isNotBlank()) {
                // Update page number for query
                pageDao.upsert(
                    GithubRemotePageEntity(
                        query = query,
                        nextPage = loadKey + 1
                    )
                )

                if (loadKey == 10) { // GitHub Search API has a limit of 10 pages
                    return MediatorResult.Success(
                        endOfPaginationReached = true
                    )
                }
            }

            MediatorResult.Success(
                endOfPaginationReached = false
            )
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}