package com.adrianosilva.githubexplorer.ui.repositories_list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.adrianosilva.githubexplorer.domain.model.Repository
import com.adrianosilva.githubexplorer.ui.theme.GithubExplorerTheme
import com.adrianosilva.githubexplorer.ui.util.ObserveAsEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@Composable
fun RepositoriesListScreenRoot(
    viewModel: RepositoriesListViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onNavigateToDetails: (repositoryId: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    RepositoriesListScreen(
        state = state,
        repositories = repositories,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                is RepositoriesListAction.GoToRepositoryDetails -> {
                    onNavigateToDetails(action.repositoryId)
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoriesListScreen(
    state: RepositoriesListUiState,
    repositories: List<Repository>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    snackbarHostState: SnackbarHostState,
    onAction: (RepositoriesListAction) -> Unit
) {
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()

    val showFab by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 2 }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            RepositoriesListTopAppBar(
                scrollBehavior = scrollBehavior,
                onAction = onAction
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = slideInHorizontally { it * 2 },
                exit = slideOutHorizontally { it * 2 }
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            gridState.animateScrollToItem(0)
                            scrollBehavior.state.heightOffset = 0f
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { onAction(RepositoriesListAction.Refresh) },
            modifier = Modifier.padding(innerPadding)
        ) {
            RepositoriesList(
                repositories = repositories,
                gridState = gridState,
                onAction = onAction,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RepositoriesListTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onAction: (RepositoriesListAction) -> Unit
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()

    LaunchedEffect(textFieldState.text) {
        delay(1000L) // wait for user to stop typing
        ensureActive()
        val query = textFieldState.text.toString()
        if (query.length >= 3) {
            onAction(RepositoriesListAction.SearchByLanguage(query))
        } else if (query.isEmpty()) {
            onAction(RepositoriesListAction.SearchByLanguage(""))
        }
    }

    val searchInputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = {
                onAction(RepositoriesListAction.SearchByLanguage(textFieldState.text.toString()))
            },
            placeholder = { Text("Search by Language") }
        )
    }

    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Column(
                modifier = Modifier
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SearchBar(
                    state = searchBarState,
                    inputField = searchInputField,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "GitHub Repositories",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    )
}

@Composable
private fun RepositoriesList(
    repositories: List<Repository>,
    gridState: LazyGridState,
    onAction: (RepositoriesListAction) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier
) {
    val isScrolledToEnd by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount <= 1) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                val trigger = 20 // trigger when 20 items from end
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - trigger
            }
        }
    }

    LaunchedEffect(isScrolledToEnd) {
        if (isScrolledToEnd) {
            onAction(RepositoriesListAction.LoadMore)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
        items(
            items = repositories,
            key = { it.id },
            contentType = { "RepositoryListItem" }
        ) {
            RepositoryListItem(
                repository = it,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                onClick = { onAction(RepositoriesListAction.GoToRepositoryDetails(it.id)) }
            )
        }
    }
}

@Composable
private fun RepositoryListItem(
    repository: Repository,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(150.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            with(sharedTransitionScope) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(repository.ownerAvatarUrl)
                            .crossfade(true)
                            .placeholderMemoryCacheKey("avatar_${repository.id}")
                            .memoryCacheKey("avatar_${repository.id}")
                            .build(),
                        contentDescription = "Owner avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .sharedElement(
                                sharedTransitionScope.rememberSharedContentState(key = "avatar_${repository.id}"),
                                animatedVisibilityScope = animatedContentScope
                            )
                            .clip(CircleShape)
                    )
                    Text(
                        text = repository.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .sharedBounds(
                                sharedTransitionScope.rememberSharedContentState(key = "fullName_${repository.id}"),
                                animatedVisibilityScope = animatedContentScope
                            )
                    )
                }

                Text(
                    text = repository.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedBounds(
                            sharedTransitionScope.rememberSharedContentState(key = "name_${repository.id}"),
                            animatedVisibilityScope = animatedContentScope
                        )
                )

                Text(
                    text = repository.description ?: "No description available.",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedBounds(
                            sharedTransitionScope.rememberSharedContentState(key = "description_${repository.id}"),
                            animatedVisibilityScope = animatedContentScope
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoriesListScreenPreview() {
    GithubExplorerTheme {
        SharedTransitionLayout {
            AnimatedContent(true) {

                RepositoriesListScreen(
                    state = RepositoriesListUiState(
                        isLoading = false,
                    ),
                    repositories = listOf(
                        Repository(
                            id = 1,
                            name = "Repo 1",
                            fullName = "User/Repo1",
                            description = "This is the first repository",
                            ownerAvatarUrl = "",
                            stargazersCount = 223,
                            forksCount = 23,
                            openIssuesCount = 11,
                            lastUpdated = ZonedDateTime.parse("2024-06-01T12:34:56Z"),
                            htmlUrl = "www.google.com",
                            language = "Kotlin, Java",
                            license = "MIT License",
                        ),
                        Repository(
                            id = 2,
                            name = "Repo 2",
                            fullName = "User/Repo2",
                            description = "This is the second repository with a lot of description to test how it looks in the UI",
                            ownerAvatarUrl = "",
                            stargazersCount = 223,
                            forksCount = 23,
                            openIssuesCount = 11,
                            lastUpdated = ZonedDateTime.parse("2024-06-01T12:34:56Z"),
                            htmlUrl = "www.google.com",
                            language = "Kotlin, Java",
                            license = "MIT License",
                        ),
                        Repository(
                            id = 3,
                            name = "Repo 3",
                            fullName = "User/Repo3",
                            description = "This is the third repository with more description",
                            ownerAvatarUrl = "",
                            stargazersCount = 223,
                            forksCount = 23,
                            openIssuesCount = 11,
                            lastUpdated = ZonedDateTime.parse("2024-06-01T12:34:56Z"),
                            htmlUrl = "www.google.com",
                            language = "Kotlin, Java",
                            license = "MIT License",
                        )
                    ),
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this,
                    snackbarHostState = SnackbarHostState(),
                    onAction = {}
                )
            }
        }
    }
}