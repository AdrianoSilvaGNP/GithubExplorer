package com.adrianosilva.githubexplorer.ui.repository_details

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.adrianosilva.githubexplorer.MainDispatcherRule
import com.adrianosilva.githubexplorer.data.local.FakeRepositoriesProvider
import com.adrianosilva.githubexplorer.generateDomainRepositoryList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repositoriesProvider: FakeRepositoriesProvider
    private lateinit var viewModel: RepositoryDetailsViewModel

    @Before
    fun setUp() {
        repositoriesProvider = FakeRepositoriesProvider()
        viewModel = RepositoryDetailsViewModel(
            repositoriesProvider = repositoriesProvider,
            repositoryId = 1
        )
    }

    @Test
    fun `Initial UI state correctness`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            assertThat(initialState.repository.id).isEqualTo(1)
            assertThat(initialState.repository.name).isEqualTo("")
        }
    }

    @Test
    fun `Observing repository updates successfully`() = runTest {
        repositoriesProvider.repositoriesFlow.value = generateDomainRepositoryList(10)

        viewModel.uiState.test {
            val initialState = awaitItem()

            assertThat(initialState.repository.id).isEqualTo(1)
            assertThat(initialState.isLoading).isFalse()

            repositoriesProvider.repositoriesFlow.update {
                val updatedList = it.toMutableList()
                updatedList[1] = updatedList[1].copy(
                    name = "Updated Repo Name",
                )
                updatedList
            }

            val updatedState = awaitItem()
            assertThat(updatedState.repository.name).isEqualTo("Updated Repo Name")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Incomplete repository data triggers fetch`() = runTest {
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isFalse()

            repositoriesProvider.repositoriesFlow.value = generateDomainRepositoryList(10).map {
                if (it.id == 1) it.copy(lastUpdated = null) else it
            }

            skipItems(1)
            val fetchingState = awaitItem()
            assertThat(fetchingState.isLoading).isTrue()

            assertThat(repositoriesProvider.fetchDetailsCount).isEqualTo(1)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Successful fetch updates UI state`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()

            repositoriesProvider.repositoriesFlow.value = generateDomainRepositoryList(10).map {
                if (it.id == 1) it.copy(lastUpdated = null) else it
            }

            skipItems(1)
            val fetchingState = awaitItem()
            assertThat(fetchingState.isLoading).isEqualTo(true)

            assertThat(repositoriesProvider.fetchDetailsCount).isEqualTo(1)

            val postFetchState = awaitItem()
            assertThat(postFetchState.isLoading).isFalse()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Network error during fetch sends an event`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()

            repositoriesProvider.shouldReturnNetworkError = true
            repositoriesProvider.repositoriesFlow.value = generateDomainRepositoryList(10).map {
                if (it.id == 1) it.copy(lastUpdated = null) else it
            }

            skipItems(1)
            val fetchingState = awaitItem()
            assertThat(fetchingState.isLoading).isTrue()

            viewModel.events.test {
                val event = awaitItem()
                assertThat(event).contains("Failed to load repository details")
            }

            val postFetchState = awaitItem()
            assertThat(postFetchState.isLoading).isFalse()

            cancelAndConsumeRemainingEvents()
        }
    }
}