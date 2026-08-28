package com.desafiomusical.app

import android.app.Application
import com.desafiomusical.app.di.AppContainer

class DesafioMusicalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
