package com.adrianosilva.githubexplorer.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class NavDestinations {
    @Serializable
    data object RepositoriesList: NavKey, NavDestinations()

    @Serializable
    data class RepositoryDetails(val repositoryId: Int): NavKey, NavDestinations()
}