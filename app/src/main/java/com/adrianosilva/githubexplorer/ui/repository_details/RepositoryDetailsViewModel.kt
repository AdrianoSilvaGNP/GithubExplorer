package com.adrianosilva.githubexplorer.ui.repository_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adrianosilva.githubexplorer.domain.model.Repository
import com.adrianosilva.githubexplorer.domain.repository.RepositoriesProvider
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class RepositoryDetailsViewModel(
    private val repositoriesProvider: RepositoriesProvider,
    private val repositoryId: Int
): ViewModel() {

    private val _uiState = MutableStateFlow(
        RepositoryDetailsUiState(
            isLoading = false,
            repository = Repository(
                id = repositoryId,
                name = "",
                fullName = "",
                description = null,
                stargazersCount = 0,
                forksCount = 0,
                openIssuesCount = 0,
                ownerAvatarUrl = "",
                lastUpdated = null,
                htmlUrl = "",
                language = "",
                license = null,
            )
        )
    )
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<String>()
    val events = eventChannel.receiveAsFlow()

    init {
        repositoriesProvider.observeRepositoryById(repositoryId)
            .onEach { repository ->
                if (repository.lastUpdated == null) {
                    Timber.w("Repository with id=$repositoryId has incomplete data")
                    fetchRepositoryDetails()
                }
                _uiState.update { it.copy(repository = repository) }
            }
            .launchIn(viewModelScope)
    }

    fun onToggleOwnerDetails() {
        _uiState.update { currentState ->
            currentState.copy(showOwnerDetails = !currentState.showOwnerDetails)
        }
    }

    private fun fetchRepositoryDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repositoriesProvider.fetchRepositoryDetails(repositoryId)

            if (result is Result.Error) {
                when (result.reason) {
                    is ErrorReason.NetworkError -> {
                        Timber.e("Network error while fetching repository details for id=$repositoryId")
                        eventChannel.send("Failed to load repository details: ${result.reason.message}")
                    }

                    is ErrorReason.NoConnection -> {
                        Timber.e("No connection while fetching repository details for id=$repositoryId")
                    }

                    else -> Timber.e("Error fetching repository details for id=$repositoryId: ${result.reason}")
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        class RepositoryDetailsViewModelFactory(
            private val repositoriesProvider: RepositoriesProvider,
            private val repositoryId: Int
        ): ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T: ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RepositoryDetailsViewModel::class.java)) {
                    return RepositoryDetailsViewModel(repositoriesProvider, repositoryId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.w("RepositoryDetailsViewModel for repoId=$repositoryId is being cleared")
    }
}