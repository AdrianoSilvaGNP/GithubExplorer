package com.adrianosilva.githubexplorer.domain.model

import java.time.ZonedDateTime

data class Repository(
    val id: Int,
    val name: String,
    val fullName: String,
    val description: String?,
    val ownerAvatarUrl: String,
    val htmlUrl: String,
    val stargazersCount: Int?,
    val forksCount: Int?,
    val openIssuesCount: Int?,
    val lastUpdated: ZonedDateTime?,
    val language: String?,
    val license: String?
)
