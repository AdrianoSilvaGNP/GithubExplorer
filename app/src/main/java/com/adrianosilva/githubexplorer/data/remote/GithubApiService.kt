package com.adrianosilva.githubexplorer.data.remote

import com.adrianosilva.githubexplorer.data.remote.dto.GitHubRepositoryDto
import com.adrianosilva.githubexplorer.data.remote.dto.GithubSearchResponseDto
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class GithubApiService(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectivityManager: AndroidConnectivityManager
) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getPublicRepositories(since: Int = 0): Result<List<GitHubRepositoryDto>> = withContext(dispatcher) {
        if (!connectivityManager.hasInternetConnection) {
            return@withContext Result.Error(ErrorReason.NoConnection)
        }
        try {
            val response = client.get("${BASE_URL}/repositories?since=$since") {
                headers.append(HttpHeaders.Accept, "application/vnd.github+json") // as per GitHub API docs
                headers.append("X-GitHub-Api-Version", "2022-11-28")
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.e("Failed to fetch repositories: ${response.status}")
                return@withContext Result.Error(ErrorReason.NetworkError(response.status.description))
            }

            return@withContext Result.Success(response.body())
        } catch (e: UnresolvedAddressException) {
            Timber.e(e, "Network error: Unable to resolve address.")
            return@withContext Result.Error(ErrorReason.NetworkError("Unable to resolve address. Is your internet connection working?"))
        } catch (e: Exception) {
            Timber.e(e, "An unexpected error occurred during the network request.")
            return@withContext Result.Error(ErrorReason.Unknown(e))
        }
    }

    suspend fun getRepositoryDetails(fullName: String): Result<GitHubRepositoryDto> = withContext(dispatcher) {
        if (!connectivityManager.hasInternetConnection) {
            return@withContext Result.Error(ErrorReason.NoConnection)
        }
        try {
            val response = client.get("${BASE_URL}/repos/$fullName") {
                headers.append(HttpHeaders.Accept, "application/vnd.github+json")
                headers.append("X-GitHub-Api-Version", "2022-11-28")
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.e("Failed to fetch repository details: ${response.status}")
                return@withContext Result.Error(ErrorReason.NetworkError(response.status.description))
            }

            return@withContext Result.Success(response.body())
        } catch (e: UnresolvedAddressException) {
            Timber.e(e, "Network error: Unable to resolve address.")
            return@withContext Result.Error(ErrorReason.NetworkError("Unable to resolve address. Is your internet connection working?"))
        } catch (e: Exception) {
            Timber.e(e, "An unexpected error occurred during the network request.")
            return@withContext Result.Error(ErrorReason.Unknown(e))
        }
    }

    suspend fun searchRepositories(query: String, page: Int = 1): Result<GithubSearchResponseDto> = withContext(dispatcher) {
        if (!connectivityManager.hasInternetConnection) {
            return@withContext Result.Error(ErrorReason.NoConnection)
        }
        try {
            val response = client.get("${BASE_URL}/search/repositories?q=$query&per_page=100&page=$page") {
                headers.append(HttpHeaders.Accept, "application/vnd.github+json")
                headers.append("X-GitHub-Api-Version", "2022-11-28")
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.e("Failed to search repositories: ${response.status}, query=$query, page=$page")
                return@withContext Result.Error(ErrorReason.NetworkError(response.status.description))
            }

            return@withContext Result.Success(response.body())
        } catch (e: UnresolvedAddressException) {
            Timber.e(e, "Network error: Unable to resolve address.")
            return@withContext Result.Error(ErrorReason.NetworkError("Unable to resolve address. Is your internet connection working?"))
        } catch (e: Exception) {
            Timber.e(e, "An unexpected error occurred during the network request.")
            return@withContext Result.Error(ErrorReason.Unknown(e))
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private const val BASE_URL = "https://api.github.com"
    }
}