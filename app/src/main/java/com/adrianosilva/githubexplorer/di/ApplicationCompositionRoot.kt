package com.adrianosilva.githubexplorer.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import com.adrianosilva.githubexplorer.data.remote.AndroidConnectivityManager
import com.adrianosilva.githubexplorer.data.local.GitHubExplorerDatabase
import com.adrianosilva.githubexplorer.data.remote.GithubApiService
import com.adrianosilva.githubexplorer.data.repository.GithubRepositoryImpl
import com.adrianosilva.githubexplorer.domain.repository.RepositoriesProvider

/**
 * Composition root for manual/pure dependency injection.
 */
class ApplicationCompositionRoot(applicationContext: Context) {

    val db = Room.databaseBuilder(
        applicationContext,
        GitHubExplorerDatabase::class.java, "Github-DB"
    ).fallbackToDestructiveMigration(true).build()

    val connectivityManager = AndroidConnectivityManager(applicationContext)

    val apiService = GithubApiService(connectivityManager = connectivityManager)

    val provider: RepositoriesProvider = GithubRepositoryImpl(
        apiService = apiService,
        githubRepositoryDao = db.githubRepositoryDao(),
        githubUserDao = db.githubUserDao(),
        githubRemotePageDao = db.githubRemotePageDao()
    )
}