package com.adrianosilva.githubexplorer.ui.repository_details

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.adrianosilva.githubexplorer.domain.model.Repository
import com.adrianosilva.githubexplorer.ui.theme.GithubExplorerTheme
import com.adrianosilva.githubexplorer.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RepositoryDetailsScreenRoot(
    viewModel: RepositoryDetailsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    RepositoryDetailsScreen(
        state = state,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        snackbarHostState = snackbarHostState,
        toggleDialog = { viewModel.onToggleOwnerDetails() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryDetailsScreen(
    state: RepositoryDetailsUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    snackbarHostState: SnackbarHostState,
    toggleDialog: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollState = rememberScrollState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
    val repository = state.repository

    if (state.showOwnerDetails) {
        OwnerInfoDialog(
            repository = repository,
            onDismissRequest = { toggleDialog() }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isLandscape) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                }
            ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    with(sharedTransitionScope) {
                        Text(
                            text = repository.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .wrapContentSize()
                                .sharedBounds(
                                    sharedTransitionScope.rememberSharedContentState(key = "name_${repository.id}"),
                                    animatedVisibilityScope = animatedContentScope
                                )
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }) { innerPadding ->

        with(sharedTransitionScope) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(
                        state = scrollState,
                        enabled = true
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(repository.ownerAvatarUrl)
                            .crossfade(true)
                            .placeholderMemoryCacheKey("avatar_${repository.id}")
                            .memoryCacheKey("avatar_${repository.id}")
                            .build(),
                        contentDescription = "Owner Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .sharedElement(
                                sharedTransitionScope.rememberSharedContentState(key = "avatar_${repository.id}"),
                                animatedVisibilityScope = animatedContentScope
                            )
                            .clickable { toggleDialog() }
                            .clip(CircleShape)
                    )
                    Text(
                        text = repository.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier
                            .sharedBounds(
                                sharedTransitionScope.rememberSharedContentState(key = "fullName_${repository.id}"),
                                animatedVisibilityScope = animatedContentScope
                            )
                    )
                }

                Text(
                    text = repository.description ?: "No description provided.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .sharedBounds(
                            sharedTransitionScope.rememberSharedContentState(key = "description_${repository.id}"),
                            animatedVisibilityScope = animatedContentScope
                        )
                )

                DetailsCard(
                    repository = repository,
                    state = state,
                    formatter = formatter
                )

                Text(
                    text = buildAnnotatedString {
                        append("View on GitHub")
                        addLink(
                            url = LinkAnnotation.Url(repository.htmlUrl),
                            start = 0,
                            end = length
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(
    repository: Repository,
    state: RepositoryDetailsUiState,
    formatter: DateTimeFormatter
) {
    HorizontalDivider()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (state.isLoading) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repository.stargazersCount?.let {
                    StatisticItem(
                        icon = Icons.Default.Star,
                        label = "Stars",
                        value = repository.stargazersCount.toString()
                    )
                }
                repository.forksCount?.let {
                    StatisticItem(
                        icon = Icons.Default.Share,
                        label = "Forks",
                        value = repository.forksCount.toString()
                    )
                }
                repository.openIssuesCount?.let {
                    StatisticItem(
                        icon = Icons.Default.Warning,
                        label = "Open Issues",
                        value = repository.openIssuesCount.toString()
                    )
                }
                repository.lastUpdated?.let {
                    StatisticItem(
                        icon = Icons.Default.Notifications,
                        label = "Last updated",
                        value = repository.lastUpdated.format(formatter)
                    )
                }
                repository.language?.let {
                    StatisticItem(
                        icon = Icons.Default.Share,
                        label = "Language",
                        value = repository.language
                    )
                }
                repository.license?.let {
                    StatisticItem(
                        icon = Icons.Default.Info,
                        label = "License",
                        value = it
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OwnerInfoDialog(
    repository: Repository,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(repository.ownerAvatarUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey("avatar_${repository.id}")
                        .memoryCacheKey("avatar_${repository.id}")
                        .build(),
                    contentDescription = "Owner Avatar, larger view",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                Text(
                    text = repository.fullName.substringBefore('/'),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RepositoryDetailsScreenPreview() {
    val previewRepo = Repository(
        id = 1,
        name = "awesome-project",
        fullName = "user/awesome-project",
        description = "This is a really cool project that does amazing things. It's written in Kotlin and demonstrates clean architecture principles.",
        ownerAvatarUrl = "",
        stargazersCount = 1234,
        forksCount = 567,
        openIssuesCount = 89,
        lastUpdated = ZonedDateTime.parse("2024-06-01T12:34:56Z"),
        htmlUrl = "",
        language = "Kotlin",
        license = "MIT"
    )
    GithubExplorerTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = true) {
                RepositoryDetailsScreen(
                    state = RepositoryDetailsUiState(
                        repository = previewRepo,
                        isLoading = false
                    ),
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this,
                    snackbarHostState = SnackbarHostState(),
                    toggleDialog = { }
                )
            }
        }
    }
}