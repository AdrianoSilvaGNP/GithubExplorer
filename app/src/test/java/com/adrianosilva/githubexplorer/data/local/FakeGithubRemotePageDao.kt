package com.adrianosilva.githubexplorer.data.local

import com.adrianosilva.githubexplorer.data.local.dao.GithubRemotePageDao
import com.adrianosilva.githubexplorer.data.local.entity.GithubRemotePageEntity

class FakeGithubRemotePageDao: GithubRemotePageDao {
    val pages = mutableListOf<GithubRemotePageEntity>()

    override suspend fun upsert(page: GithubRemotePageEntity) {
        val index = pages.indexOfFirst { it.query == page.query }
        if (index >= 0) {
            pages[index] = page
        } else {
            pages.add(page)
        }
    }

    override suspend fun getByQuery(query: String): GithubRemotePageEntity? {
        return pages.firstOrNull { it.query == query }
    }

    override suspend fun deleteByQuery(query: String) {
        pages.removeAll { it.query == query }
    }
}