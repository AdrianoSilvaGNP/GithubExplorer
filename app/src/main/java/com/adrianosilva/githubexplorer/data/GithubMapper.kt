package com.adrianosilva.githubexplorer.data

import com.adrianosilva.githubexplorer.data.local.entity.GithubRepositoryEntity
import com.adrianosilva.githubexplorer.data.local.entity.GithubUserEntity
import com.adrianosilva.githubexplorer.data.remote.dto.GitHubRepositoryDto
import com.adrianosilva.githubexplorer.domain.model.Repository
import java.time.ZonedDateTime

fun GitHubRepositoryDto.mapToDomain(): Repository {
    return Repository(
        id = this.id,
        name = this.name,
        fullName = this.fullName,
        description = this.description,
        ownerAvatarUrl = this.owner.avatarUrl,
        stargazersCount = this.stargazersCount ?: 0,
        forksCount = this.forksCount ?: 0,
        openIssuesCount = this.openIssuesCount ?: 0,
        lastUpdated = if (this.updatedAt != null) ZonedDateTime.parse(this.updatedAt) else null,
        htmlUrl = this.htmlUrl,
        language = this.language ?: "Unknown",
        license = this.license?.name
    )
}

fun GitHubRepositoryDto.deconstructToEntities(): Pair<GithubRepositoryEntity, GithubUserEntity> {
    val userEntity = GithubUserEntity(
        id = this.owner.id,
        username = this.owner.login,
        avatarUrl = this.owner.avatarUrl
    )
    val repo = GithubRepositoryEntity(
        id = this.id,
        name = this.name,
        fullName = this.fullName,
        description = this.description,
        ownerAvatarUrl = this.owner.avatarUrl,
        htmlUrl = this.htmlUrl,
        stargazersCount = this.stargazersCount,
        forksCount = this.forksCount,
        openIssuesCount = this.openIssuesCount,
        lastUpdated = this.updatedAt,
        language = this.language,
        license = this.license?.name
    )
    return Pair(repo, userEntity)
}

fun GithubRepositoryEntity.mapToDomain(): Repository {
    return Repository(
        id = this.id,
        name = this.name,
        fullName = this.fullName,
        description = this.description,
        ownerAvatarUrl = this.ownerAvatarUrl,
        htmlUrl = this.htmlUrl,
        stargazersCount = this.stargazersCount,
        forksCount = this.forksCount,
        openIssuesCount = this.openIssuesCount,
        lastUpdated = if (this.lastUpdated != null) ZonedDateTime.parse(this.lastUpdated) else null,
        language = this.language,
        license = this.license
    )
}