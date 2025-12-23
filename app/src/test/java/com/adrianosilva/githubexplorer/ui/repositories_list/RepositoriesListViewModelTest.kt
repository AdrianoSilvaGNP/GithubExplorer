package com.adrianosilva.githubexplorer.ui.repositories_list

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.adrianosilva.githubexplorer.MainDispatcherRule
import com.adrianosilva.githubexplorer.data.local.FakeRepositoriesProvider
import com.adrianosilva.githubexplorer.generateDomainRepositoryList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repositoriesProvider: FakeRepositoriesProvider
    private lateinit var viewModel: RepositoriesListViewModel

    @Before
    fun setUp() {
        repositoriesProvider = FakeRepositoriesProvider()
        viewModel = RepositoriesListViewModel(repositoriesProvider)
    }

    @Test
    fun `getUiState   initial state validation`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState).isEqualTo(RepositoriesListUiState())
        }
    }

    @Test
    fun `getUiState   updates isLoading during refresh`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isEqualTo(false)
            advanceUntilIdle()

            viewModel.onAction(RepositoriesListAction.Refresh)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isEqualTo(true)

            val finalState = awaitItem()
            assertThat(finalState.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `getUiState   updates isLoading during load more`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isEqualTo(false)
            advanceUntilIdle()

            viewModel.onAction(RepositoriesListAction.LoadMore)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isEqualTo(true)

            val finalState = awaitItem()
            assertThat(finalState.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `getUiState   updates isLoading during search`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isEqualTo(false)
            advanceUntilIdle()

            viewModel.onAction(RepositoriesListAction.SearchByLanguage("Kotlin"))

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isEqualTo(true)

            val finalState = awaitItem()
            assertThat(finalState.isLoading).isEqualTo(false)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getUiState   updates isSearching on non blank search query`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isSearching).isEqualTo(false)

            viewModel.onAction(RepositoriesListAction.SearchByLanguage("Kotlin"))

            val searchingState = awaitItem()
            println("Searching State: $searchingState")
            assertThat(searchingState.isSearching).isEqualTo(true)
        }
    }

    @Test
    fun `getUiState   updates isSearching on blank search query`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isSearching).isEqualTo(false)

            viewModel.onAction(RepositoriesListAction.SearchByLanguage("Kotlin"))
            val searchingState = awaitItem()
            assertThat(searchingState.isSearching).isEqualTo(true)

            viewModel.onAction(RepositoriesListAction.SearchByLanguage(""))
            val notSearchingState = awaitItem()
            assertThat(notSearchingState.isSearching).isEqualTo(false)
        }
    }

    @Test
    fun `getEvents   emits network error on refresh failure`() = runTest {
        repositoriesProvider.shouldReturnNetworkError = true
        viewModel.events.test {
            viewModel.onAction(RepositoriesListAction.Refresh)

            val event = awaitItem()
            assertThat(event).isEqualTo("Network error: Simulated network error")

            cancelAndConsumeRemainingEvents()
        }
    }


    @Test
    fun `getEvents   does not emit on success`() = runTest {
        viewModel.events.test {
            viewModel.onAction(RepositoriesListAction.Refresh)

            expectNoEvents()
        }
    }

    @Test
    fun `getRepositories   initial value check`() = runTest {
        viewModel.repositories.test {
            val initialRepos = awaitItem()
            assertThat(initialRepos).isEqualTo(emptyList())
        }
    }

    @Test
    fun `getRepositories   updates on query change`() = runTest {
        // When the search query is blank, verify that the repositories flow switches to observe and emit data from repositoriesProvider.observeRepositories().
        repositoriesProvider.repositoriesFlow.value = generateDomainRepositoryList(50)

        viewModel.repositories.test {
            awaitItem() // initial empty list
            val initialRepos = awaitItem()
            assertThat(initialRepos.size).isEqualTo(50)

            viewModel.onAction(RepositoriesListAction.SearchByLanguage("Kotlin"))

            val searchedRepos = awaitItem()
            assertThat(searchedRepos).isNotEqualTo(initialRepos)
            assertThat(searchedRepos).hasSize(25)

            viewModel.onAction(RepositoriesListAction.SearchByLanguage(""))

            val finalRepos = awaitItem()
            assertThat(finalRepos).isEqualTo(initialRepos)
            assertThat(finalRepos).hasSize(50)
        }
    }

    @Test
    fun `onAction Refresh  triggers refresh`() = runTest {
        viewModel.uiState.test {
            val loadingState = awaitItem() // initial state
            assertThat(loadingState.isLoading).isFalse()
            advanceUntilIdle()

            viewModel.onAction(RepositoriesListAction.Refresh)

            val refreshingState = awaitItem()
            assertThat(refreshingState.isLoading).isTrue()

            assertThat(repositoriesProvider.refreshCallCount).isEqualTo(1)

            val finalState = awaitItem()
            assertThat(finalState.isLoading).isFalse()
        }
    }

    @Test
    fun `onAction LoadMore  triggers fetch more when not searching`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()

            viewModel.onAction(RepositoriesListAction.LoadMore)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            assertThat(repositoriesProvider.fetchMoreCallCount).isEqualTo(1)

            val finalState = awaitItem()
            assertThat(finalState.isLoading).isFalse()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAction LoadMore  triggers fetch by language when searching`() = runTest {
        repositoriesProvider.repositoriesFlow.value = generateDomainRepositoryList(50)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()

            viewModel.onAction(RepositoriesListAction.SearchByLanguage("Kotlin"))
            advanceUntilIdle()
            val searchingState = awaitItem()
            assertThat(searchingState.isSearching).isTrue()

            viewModel.onAction(RepositoriesListAction.LoadMore)

            advanceUntilIdle()
            val finalState = awaitItem()
            assertThat(finalState.isSearching).isTrue()
            assertThat(repositoriesProvider.fetchSearchCount).isEqualTo(2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Concurrent onAction calls handling`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            advanceUntilIdle()

            viewModel.onAction(RepositoriesListAction.Refresh)
            viewModel.onAction(RepositoriesListAction.SearchByLanguage("Kotlin"))

            var loadingState = awaitItem()
            println("Loading State: $loadingState")
            assertThat(loadingState.isLoading).isTrue()

            awaitItem()
            awaitItem()
            awaitItem()
            loadingState = awaitItem()
            println("Loading State After: $loadingState")

            assertThat(loadingState.isLoading).isFalse()
            assertThat(loadingState.isSearching).isTrue()

            assertThat(repositoriesProvider.fetchSearchCount).isEqualTo(1)
            assertThat(repositoriesProvider.refreshCallCount).isEqualTo(2)

            cancelAndIgnoreRemainingEvents()
        }
    }
}