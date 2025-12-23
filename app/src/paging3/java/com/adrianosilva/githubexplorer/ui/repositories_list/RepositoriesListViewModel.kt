package com.adrianosilva.githubexplorer.ui.repositories_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.adrianosilva.githubexplorer.domain.repository.RepositoriesProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class RepositoriesListViewModel(
    private val repositoriesProvider: RepositoriesProvider,
): ViewModel() {

    private val eventChannel = Channel<String>()
    val events = eventChannel.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val paging3Repos = _searchQuery.flatMapLatest { query ->
        repositoriesProvider.observePaginatedRepositories(query)
    }.cachedIn(viewModelScope)

    fun onAction(action: RepositoriesListAction) {
        when (action) {
            is RepositoriesListAction.SearchByLanguage -> {
                if (_searchQuery.value != action.language) {
                    Timber.d("Searching repositories by language: ${action.language}")
                    _searchQuery.update { action.language }
                }
            }

            else -> Unit
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