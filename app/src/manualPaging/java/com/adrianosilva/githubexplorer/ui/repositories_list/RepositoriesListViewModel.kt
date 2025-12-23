package com.adrianosilva.githubexplorer.ui.repositories_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adrianosilva.githubexplorer.domain.repository.RepositoriesProvider
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class RepositoriesListViewModel(
    private val repositoriesProvider: RepositoriesProvider,
): ViewModel() {

    private val _uiState = MutableStateFlow(RepositoriesListUiState())
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<String>()
    val events = eventChannel.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    private var searchJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val repositories = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repositoriesProvider.observeRepositories()
        } else {
            repositoriesProvider.observeRepositoriesByLanguage(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            if (repositoriesProvider.isRepositoryListEmpty()) {
                refreshRepositories()
            }
        }
    }

    fun onAction(action: RepositoriesListAction) {
        when (action) {
            RepositoriesListAction.Refresh -> refreshRepositories()
            RepositoriesListAction.LoadMore -> {
                if (_uiState.value.isSearching) {
                    if (searchJob?.isActive == true) return
                    fetchRepositoriesByLanguage(_searchQuery.value)
                } else {
                    fetchMoreRepositories()
                }
            }

            is RepositoriesListAction.SearchByLanguage -> {
                if (action.language.isNotBlank()) {
                    _uiState.update { it.copy(isSearching = true) }
                    if (searchJob?.isActive == true) return
                    fetchRepositoriesByLanguage(action.language)
                } else {
                    _uiState.update { it.copy(isSearching = false) }
                }
                _searchQuery.update { action.language }
            }

            else -> Unit
        }
    }

    private fun refreshRepositories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repositoriesProvider.refreshRepositories()
            if (result is Result.Error) {
                when (result.reason) {
                    is ErrorReason.NetworkError -> {
                        Timber.e("Network error: ${result.reason.message}")
                        eventChannel.send("Network error: ${result.reason.message}")
                    }

                    is ErrorReason.Unknown -> {
                        Timber.e("Unknown error: ${result.reason.exception}")
                        eventChannel.send("Unknown error occurred")
                    }

                    ErrorReason.NoData -> {
                        Timber.e("No data available")
                        eventChannel.send("No data available")
                    }

                    else -> {}
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun fetchMoreRepositories() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repositoriesProvider.fetchNextPageRepositories()
            if (result is Result.Error) {
                when (result.reason) {
                    is ErrorReason.NetworkError -> {
                        Timber.e("Network error: ${result.reason.message}")
                        eventChannel.send("Network error: ${result.reason.message}")
                    }

                    is ErrorReason.Unknown -> {
                        Timber.e("Unknown error: ${result.reason.exception}")
                        eventChannel.send("Unknown error occurred")
                    }

                    ErrorReason.NoData -> {
                        Timber.e("No data available")
                        eventChannel.send("No data available")
                    }

                    else -> {}
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun fetchRepositoriesByLanguage(language: String) {
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repositoriesProvider.fetchRepositoriesByLanguage(language)
            if (result is Result.Error) {
                when (result.reason) {
                    is ErrorReason.NetworkError -> {
                        Timber.e("Network error: ${result.reason.message}")
                        eventChannel.send("Network error: ${result.reason.message}")
                    }

                    is ErrorReason.Unknown -> {
                        Timber.e("Unknown error: ${result.reason.exception}")
                        eventChannel.send("Unknown error occurred")
                    }

                    ErrorReason.NoData -> {
                        Timber.e("No data available")
                        eventChannel.send("No data available")
                    }

                    else -> {}
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        class RepositoriesListViewModelFactory(
            private val repositoriesProvider: RepositoriesProvider
        ): ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T: ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RepositoriesListViewModel::class.java)) {
                    return RepositoriesListViewModel(repositoriesProvider) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}