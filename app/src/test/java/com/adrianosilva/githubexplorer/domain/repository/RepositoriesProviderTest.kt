package com.adrianosilva.githubexplorer.domain.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import com.adrianosilva.githubexplorer.MainDispatcherRule
import com.adrianosilva.githubexplorer.data.local.FakeGithubRemotePageDao
import com.adrianosilva.githubexplorer.data.local.FakeGithubRepositoryDao
import com.adrianosilva.githubexplorer.data.local.FakeGithubUserDao
import com.adrianosilva.githubexplorer.data.remote.GithubApiService
import com.adrianosilva.githubexplorer.data.remote.dto.GithubSearchResponseDto
import com.adrianosilva.githubexplorer.data.repository.GithubRepositoryImpl
import com.adrianosilva.githubexplorer.domain.util.ErrorReason
import com.adrianosilva.githubexplorer.domain.util.Result
import com.adrianosilva.githubexplorer.generateRepositoriesEntityList
import com.adrianosilva.githubexplorer.generateRepositoryDtoList
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesProviderTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockApiService: GithubApiService
    private lateinit var fakeRepositoryDao: FakeGithubRepositoryDao
    private lateinit var fakeUserDao: FakeGithubUserDao
    private lateinit var fakeRemotePageDao: FakeGithubRemotePageDao

    private lateinit var repositoriesProvider: RepositoriesProvider

    @Before
    fun setUp() {
        mockApiService = mockk()
        fakeRepositoryDao = FakeGithubRepositoryDao()
        fakeUserDao = FakeGithubUserDao()
        fakeRemotePageDao = FakeGithubRemotePageDao()

        repositoriesProvider = GithubRepositoryImpl(
            apiService = mockApiService,
            githubRepositoryDao = fakeRepositoryDao,
            githubUserDao = fakeUserDao,
            githubRemotePageDao = fakeRemotePageDao,
            dispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun `observeRepositories initial empty state`() = runTest {
        repositoriesProvider.observeRepositories().test {
            val repositories = awaitItem()
            assertThat(repositories).isEmpty()
        }
    }

    @Test
    fun `observeRepositories successful data emission`() = runTest {
        val entities = generateRepositoriesEntityList(20)
        fakeRepositoryDao.upsertAll(entities)

        repositoriesProvider.observeRepositories().test {
            val repositories = awaitItem()
            assertThat(repositories).hasSize(20)
        }
    }

    @Test
    fun `observeRepositories data updates`() = runTest {
        val entities = generateRepositoriesEntityList(20)
        fakeRepositoryDao.upsertAll(entities)
        repositoriesProvider.observeRepositories().test {
            val initialRepositories = awaitItem()
            assertThat(initialRepositories).hasSize(20)

            val newEntities = generateRepositoriesEntityList(10, fromIndex = 20)
            fakeRepositoryDao.upsertAll(newEntities)

            val updatedRepositories = awaitItem()
            Timber.d("Updated repositories: $updatedRepositories")
            assertThat(updatedRepositories).hasSize(30)
        }
    }

    @Test
    fun `getRepositoryById successful retrieval`() = runTest {
        val entities = generateRepositoriesEntityList(5)
        fakeRepositoryDao.upsertAll(entities)

        val targetId = entities[2].id
        val repository = repositoriesProvider.getRepositoryById(targetId)

        assertThat(repository).isNotNull()
        assertThat(repository!!.id).isEqualTo(targetId)
    }

    @Test
    fun `getRepositoryById non existent ID`() = runTest {
        val entities = generateRepositoriesEntityList(5)
        fakeRepositoryDao.upsertAll(entities)

        val nonExistentId = 6
        val repository = repositoriesProvider.getRepositoryById(nonExistentId)

        assertThat(repository).isEqualTo(null)
    }


    @Test
    fun `observeRepositoriesByLanguage successful retrieval`() = runTest {
        val entities = generateRepositoriesEntityList(10)
        fakeRepositoryDao.upsertAll(entities)

        repositoriesProvider.observeRepositoriesByLanguage("Kotlin").test {
            val kotlinRepositories = awaitItem()
            assertThat(kotlinRepositories).hasSize(5)
            for (repo in kotlinRepositories) {
                assertThat(repo.language).isEqualTo("Kotlin")
            }
        }
    }

    @Test
    fun `observeRepositoriesByLanguage empty results`() = runTest {
        val entities = generateRepositoriesEntityList(10)
        fakeRepositoryDao.upsertAll(entities)

        repositoriesProvider.observeRepositoriesByLanguage("Swift").test {
            val swiftRepositories = awaitItem()
            assertThat(swiftRepositories).isEmpty()
        }
    }

    @Test
    fun `observeRepositoryById successful retrieval`() = runTest {
        val entities = generateRepositoriesEntityList(5)
        fakeRepositoryDao.upsertAll(entities)

        val targetId = entities[1].id

        repositoriesProvider.observeRepositoryById(targetId).test {
            val repository = awaitItem()
            assertThat(repository).isNotNull()
            assertThat(repository.id).isEqualTo(targetId)
        }
    }

    @Test
    fun `observeRepositoryById non existent ID`() = runTest {
        val entities = generateRepositoriesEntityList(5)
        fakeRepositoryDao.upsertAll(entities)

        val nonExistentId = 6
        repositoriesProvider.observeRepositoryById(nonExistentId).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(NoSuchElementException::class.java)
        }
    }

    @Test
    fun `observeRepositoryById data updates`() = runTest {
        val entities = generateRepositoriesEntityList(5)
        fakeRepositoryDao.upsertAll(entities)

        val targetId = entities[3].id

        repositoriesProvider.observeRepositoryById(targetId).test {
            val initialRepository = awaitItem()
            assertThat(initialRepository).isNotNull()
            assertThat(initialRepository.id).isEqualTo(targetId)

            val updatedEntity = entities[3].copy(description = "Updated description")
            fakeRepositoryDao.upsert(updatedEntity)

            val updatedRepository = awaitItem()
            assertThat(updatedRepository).isNotNull()
            assertThat(updatedRepository.description).isEqualTo("Updated description")
        }
    }

    @Test
    fun `refreshRepositories successful refresh`() = runTest {
        val fakeApiData = generateRepositoryDtoList(100)

        coEvery {
            mockApiService.getPublicRepositories()
        } returns Result.Success(fakeApiData)

        val result = repositoriesProvider.refreshRepositories()
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val storedRepositories = fakeRepositoryDao.getAllRepositories()
        assertThat(storedRepositories).hasSize(100)

        val result2 = repositoriesProvider.refreshRepositories()
        assertThat(result2).isNotNull()
        assertThat(result2).isInstanceOf(Result.Success::class.java)

        val storedRepositories2 = fakeRepositoryDao.getAllRepositories()
        assertThat(storedRepositories2).hasSize(100)
    }

    @Test
    fun `refreshRepositories network error`() = runTest {
        coEvery {
            mockApiService.getPublicRepositories()
        } returns Result.Error(ErrorReason.NetworkError("Unable to connect"))

        val result = repositoriesProvider.refreshRepositories()
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Error::class.java)
        (result as Result.Error).let {
            assertThat(it.reason).isInstanceOf(ErrorReason.NetworkError::class.java)
        }

        val storedRepositories = fakeRepositoryDao.getAllRepositories()
        assertThat(storedRepositories).isEmpty()
    }

    @Test
    fun `fetchNextPageRepositories successful fetch`() = runTest {
        coEvery {
            mockApiService.getPublicRepositories()
        } returns Result.Success(generateRepositoryDtoList(100))

        coEvery {
            mockApiService.getPublicRepositories(99)
        } returns Result.Success(generateRepositoryDtoList(100, 100))

        val result = repositoriesProvider.fetchNextPageRepositories()
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val storedRepositories = fakeRepositoryDao.getAllRepositories()
        assertThat(storedRepositories).hasSize(100)

        val result2 = repositoriesProvider.fetchNextPageRepositories()
        assertThat(result2).isNotNull()
        assertThat(result2).isInstanceOf(Result.Success::class.java)

        val storedRepositories2 = fakeRepositoryDao.getAllRepositories()
        assertThat(storedRepositories2).hasSize(200)
    }

    @Test
    fun `fetchNextPageRepositories network error`() = runTest {
        coEvery {
            mockApiService.getPublicRepositories()
        } returns Result.Error(ErrorReason.NetworkError("Unable to connect"))

        val result = repositoriesProvider.fetchNextPageRepositories()
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Error::class.java)
        (result as Result.Error).let {
            assertThat(it.reason).isInstanceOf(ErrorReason.NetworkError::class.java)
        }
    }

    @Test
    fun `fetchRepositoryDetails successful fetch`() = runTest {
        val entities = generateRepositoriesEntityList(5)
        fakeRepositoryDao.upsertAll(entities)
        val targetId = entities[3].id

        coEvery {
            mockApiService.getRepositoryDetails(any())
        } answers {
            val dto = generateRepositoryDtoList(1, fromIndex = 3).first()
            Result.Success(dto)
        }

        val result = repositoriesProvider.fetchRepositoryDetails(targetId)
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val updatedEntity = fakeRepositoryDao.getRepositoryById(targetId)
        assertThat(updatedEntity).isNotNull()
        assertThat(updatedEntity!!.fullName).isEqualTo(entities[3].fullName)
        assertThat(updatedEntity).isNotEqualTo(entities[3])

    }

    @Test
    fun `fetchRepositoryDetails non existent ID`() = runTest {
        generateRepositoriesEntityList(50).let {
            fakeRepositoryDao.upsertAll(it)
        }

        val nonExistentId = 999
        val result = repositoriesProvider.fetchRepositoryDetails(nonExistentId)
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Error::class.java)
        (result as Result.Error).let {
            assertThat(it.reason).isInstanceOf(ErrorReason.NoData::class.java)
        }
    }

    @Test
    fun `fetchRepositoryDetails network error`() = runTest {
        generateRepositoriesEntityList(50).let {
            fakeRepositoryDao.upsertAll(it)
        }

        val targetId = 25
        coEvery {
            mockApiService.getRepositoryDetails(any())
        } returns Result.Error(ErrorReason.NetworkError("Unable to connect"))

        val result = repositoriesProvider.fetchRepositoryDetails(targetId)
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(Result.Error::class.java)
        (result as Result.Error).let {
            assertThat(it.reason).isInstanceOf(ErrorReason.NetworkError::class.java)
        }
    }

    @Test
    fun `fetchRepositoriesByLanguage successful fetch`() = runTest {
        fakeRepositoryDao.upsertAll(generateRepositoriesEntityList(50))

        coEvery {
            mockApiService.searchRepositories("language:kotlin", 1)
        } returns Result.Success(
            GithubSearchResponseDto(
                totalCount = 50,
                incompleteResults = false,
                items = generateRepositoryDtoList(25, fromIndex = 50).map {
                    it.copy(language = "Kotlin")
                }
            )
        )

        fakeRepositoryDao.observeAllRepositoriesByLanguage("kotlin").test {
            val firstEmission = awaitItem()
            assertThat(firstEmission).hasSize(25)

            val result = repositoriesProvider.fetchRepositoriesByLanguage("kotlin")
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val secondEmission = awaitItem()
            assertThat(secondEmission).hasSize(50)
            for (repo in secondEmission) {
                assertThat(repo.language).isEqualTo("Kotlin")
            }
        }
    }

    @Test
    fun `fetchRepositoriesByLanguage empty string`() = runTest {
        // Test the function's behavior with an empty language string. It should likely return a failure or handle it as an invalid input.
        fakeRepositoryDao.upsertAll(generateRepositoriesEntityList(50))

        val result = repositoriesProvider.fetchRepositoriesByLanguage("")

        assertThat(result).isInstanceOf(Result.Error::class.java)
        (result as Result.Error).let {
            assertThat(it.reason).isInstanceOf(ErrorReason.NoData::class.java)
        }
    }

    @Test
    fun `fetchRepositoriesByLanguage network error`() = runTest {
        // Verify the function returns Result.failure when fetching by language fails due to a network error.
        fakeRepositoryDao.upsertAll(generateRepositoriesEntityList(50))

        coEvery {
            mockApiService.searchRepositories("language:kotlin", 1)
        } returns Result.Error(ErrorReason.NetworkError("Unable to connect"))

        val result = repositoriesProvider.fetchRepositoriesByLanguage("kotlin")
        assertThat(result).isInstanceOf(Result.Error::class.java)
        (result as Result.Error).let {
            assertThat(it.reason).isInstanceOf(ErrorReason.NetworkError::class.java)
        }
    }

    @Test
    fun `isRepositoryListEmpty when empty`() = runTest {
        // Verify the function returns true when the local repository data source is empty.
        val result = repositoriesProvider.isRepositoryListEmpty()
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `isRepositoryListEmpty when not empty`() = runTest {
        // Verify the function returns false when the local repository data source contains at least one repository.
        fakeRepositoryDao.upsertAll(generateRepositoriesEntityList(10))

        val result = repositoriesProvider.isRepositoryListEmpty()
        assertThat(result).isEqualTo(false)
    }

}