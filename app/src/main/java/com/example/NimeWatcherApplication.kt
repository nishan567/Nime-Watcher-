package com.example

import android.app.Application
import com.example.di.AppContainer

class NimeWatcherApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(applicationContext)
    }
}
