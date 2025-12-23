package com.adrianosilva.githubexplorer

import com.adrianosilva.githubexplorer.data.local.entity.GithubRepositoryEntity
import com.adrianosilva.githubexplorer.data.remote.dto.GitHubRepositoryDto
import com.adrianosilva.githubexplorer.domain.model.Repository
import io.mockk.mockk
import kotlin.random.Random

fun generateRepositoriesEntityList(count: Int, fromIndex: Int = 0): List<GithubRepositoryEntity> {
    val list = mutableListOf<GithubRepositoryEntity>()
    val newCount = fromIndex + count
    for (index in fromIndex until newCount) {
        list.add(
            GithubRepositoryEntity(
                id = index,
                name = "Repository $index",
                fullName = "Full Repository $index",
                description = "Description for repository $index",
                htmlUrl = "google.com",
                ownerAvatarUrl = "avatar.com",
                stargazersCount = Random.nextInt(0, 1000),
                forksCount = Random.nextInt(0, 1000),
                openIssuesCount = Random.nextInt(0, 1000),
                lastUpdated = "2025-12-24T00:24:46Z",
                language = if (index % 2 == 0) "Kotlin" else "Java",
                license = "MIT"
            )
        )
    }
    return list
}

fun generateRepositoryDtoList(count: Int, fromIndex: Int = 0): List<GitHubRepositoryDto> {
    val list = mutableListOf<GitHubRepositoryDto>()
    val newCount = fromIndex + count
    for (index in fromIndex until newCount) {
        list.add(
            GitHubRepositoryDto(
                id = index,
                nodeId = "nodeId_$index",
                name = "Repository $index",
                fullName = "Full Repository $index",
                owner = mockk(relaxed = true),
                archiveUrl = "",
                assigneesUrl = "",
                blobsUrl = "",
                branchesUrl = "",
                collaboratorsUrl = "",
                commentsUrl = "",
                commitsUrl = "",
                compareUrl = "",
                contentsUrl = "",
                contributorsUrl = "",
                deploymentsUrl = "",
                description = "Description for repository $index",
                downloadsUrl = "",
                eventsUrl = "",
                fork = false,
                forksUrl = "",
                gitCommitsUrl = "",
                gitRefsUrl = "",
                gitTagsUrl = "",
                hooksUrl = "",
                htmlUrl = "google.com",
                issueCommentUrl = "",
                issueEventsUrl = "",
                issuesUrl = "",
                keysUrl = "",
                labelsUrl = "",
                languagesUrl = "",
                mergesUrl = "",
                milestonesUrl = "",
                notificationsUrl = "",
                private = false,
                pullsUrl = "",
                releasesUrl = "",
                stargazersUrl = "",
                statusesUrl = "",
                subscribersUrl = "",
                subscriptionUrl = "",
                tagsUrl = "",
                teamsUrl = "",
                treesUrl = "",
                url = "",
                stargazersCount = Random.nextInt(0, 1000),
                forksCount = Random.nextInt(0, 1000),
                openIssuesCount = Random.nextInt(0, 1000),
                updatedAt = "2025-12-24T00:24:46Z",
                language = if (index % 2 == 0) "Kotlin" else "Java",
                license = mockk(relaxed = true),
            )
        )
    }
    return list
}

fun generateDomainRepositoryList(count: Int, fromIndex: Int = 0): List<Repository> {
    val list = mutableListOf<Repository>()
    val newCount = fromIndex + count
    for (index in fromIndex until newCount) {
        list.add(
            Repository(
                id = index,
                name = "Repository $index",
                fullName = "Full Repository $index",
                description = "Description for repository $index",
                ownerAvatarUrl = "avatar.com",
                htmlUrl = "google.com",
                stargazersCount = Random.nextInt(0, 1000),
                forksCount = Random.nextInt(0, 1000),
                openIssuesCount = Random.nextInt(0, 1000),
                lastUpdated = null,
                language = if (index % 2 == 0) "Kotlin" else "Java",
                license = "MIT"
            )
        )
    }
    return list
}
