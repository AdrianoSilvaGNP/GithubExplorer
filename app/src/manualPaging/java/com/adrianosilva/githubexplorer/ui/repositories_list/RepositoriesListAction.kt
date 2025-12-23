package com.adrianosilva.githubexplorer.ui.repositories_list

sealed class RepositoriesListAction {
    object Refresh: RepositoriesListAction()
    object LoadMore: RepositoriesListAction()
    data class SearchByLanguage(val language: String): RepositoriesListAction()
    data class GoToRepositoryDetails(val repositoryId: Int): RepositoriesListAction()
}