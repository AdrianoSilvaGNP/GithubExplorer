package com.adrianosilva.githubexplorer.ui.repository_details

import com.adrianosilva.githubexplorer.domain.model.Repository

data class RepositoryDetailsUiState(
    val isLoading: Boolean = false,
    val showOwnerDetails: Boolean = false,
    val repository: Repository
)
