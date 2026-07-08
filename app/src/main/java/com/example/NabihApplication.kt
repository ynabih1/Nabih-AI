package com.example

import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer

import android.app.Application

class NabihApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
