package com.adrianosilva.githubexplorer

import android.app.Application
import com.adrianosilva.githubexplorer.di.ApplicationCompositionRoot
import timber.log.Timber

class GitHubExplorerApp: Application() {

    lateinit var applicationCompositionRoot: ApplicationCompositionRoot

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        applicationCompositionRoot = ApplicationCompositionRoot(this)
    }
}