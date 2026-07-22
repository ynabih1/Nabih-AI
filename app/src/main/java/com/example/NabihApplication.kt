package com.example

import com.example.di.AppContainer
import com.example.di.DefaultAppContainer

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

class NabihApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        
        // NOTE FOR DEVELOPER (Manual steps outside AI Studio):
        // 1. Enable Play Integrity API in Google Cloud Console for the project 'nabih-ai'
        //    (https://console.cloud.google.com/apis/library/playintegrity.googleapis.com)
        // 2. Go to App Check in Firebase Console and verify the app shows as "Registered"
        //    after deploying and running this code on a real device or emulator.
    }
}
