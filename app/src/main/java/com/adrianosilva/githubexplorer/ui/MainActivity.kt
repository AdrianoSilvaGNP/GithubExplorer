package com.adrianosilva.githubexplorer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.adrianosilva.githubexplorer.GitHubExplorerApp
import com.adrianosilva.githubexplorer.ui.navigation.NavDestinations
import com.adrianosilva.githubexplorer.ui.repositories_list.RepositoriesListScreenRoot
import com.adrianosilva.githubexplorer.ui.repositories_list.RepositoriesListViewModel
import com.adrianosilva.githubexplorer.ui.repository_details.RepositoryDetailsScreenRoot
import com.adrianosilva.githubexplorer.ui.repository_details.RepositoryDetailsViewModel
import com.adrianosilva.githubexplorer.ui.theme.GithubExplorerTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get dependencies
        val applicationCompositionRoot = (application as GitHubExplorerApp).applicationCompositionRoot
        val provider = applicationCompositionRoot.provider

        enableEdgeToEdge()
        setContent {
            val backStack = rememberNavBackStack(NavDestinations.RepositoriesList)

            GithubExplorerTheme {
                SharedTransitionLayout {
                    NavDisplay(
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator() // scope viewModels to nav entries
                        ),
                        sharedTransitionScope = this,
                        backStack = backStack,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it }
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it }
                            )
                        },
                        popTransitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { -it }
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it }
                            )
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { -it }
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it }
                            )
                        },
                        entryProvider = entryProvider {
                            entry<NavDestinations.RepositoriesList> {
                                RepositoriesListScreenRoot(
                                    viewModel = viewModel(
                                        factory = RepositoriesListViewModel.Companion.RepositoriesListViewModelFactory(
                                            provider
                                        )
                                    ),
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedContentScope = LocalNavAnimatedContentScope.current,
                                    onNavigateToDetails = { repoId ->
                                        backStack.add(NavDestinations.RepositoryDetails(repoId))
                                    }
                                )
                            }

                            entry<NavDestinations.RepositoryDetails> { destination ->
                                RepositoryDetailsScreenRoot(
                                    viewModel = viewModel(
                                        factory = RepositoryDetailsViewModel.Companion.RepositoryDetailsViewModelFactory(
                                            provider, destination.repositoryId
                                        ),
                                    ),
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedContentScope = LocalNavAnimatedContentScope.current,
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}