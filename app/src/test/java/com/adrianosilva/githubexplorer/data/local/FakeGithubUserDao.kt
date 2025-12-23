package com.adrianosilva.githubexplorer.data.local

import com.adrianosilva.githubexplorer.data.local.dao.GithubUserDao
import com.adrianosilva.githubexplorer.data.local.entity.GithubUserEntity

class FakeGithubUserDao: GithubUserDao {
    private val users = mutableListOf<GithubUserEntity>()

    override suspend fun upsert(user: GithubUserEntity) {
        val index = users.indexOfFirst { it.id == user.id }
        if (index >= 0) {
            users[index] = user
        } else {
            users.add(user)
        }
    }

    override suspend fun upsertAll(users: List<GithubUserEntity>) {
        for (user in users) {
            val index = this.users.indexOfFirst { it.id == user.id }
            if (index >= 0) {
                this.users[index] = user
            } else {
                this.users.add(user)
            }
        }
    }

    override suspend fun getAllUsers(): List<GithubUserEntity> {
        return users
    }
}