package com.example
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp

import com.example.di.ViewModelFactory
import com.example.models.AppLanguage
import com.example.models.AppTheme
import com.example.ui.theme.NabihTheme
import com.example.auth.AccountScreen

import com.example.auth.LoginScreen
import com.example.chat.logic.ChatViewModel
import com.example.chat.logic.HomeViewModel
import com.example.chat.ui.MainScreen
import com.example.settings.general.SettingsScreen
import com.example.settings.profile.SettingsViewModel
import com.example.settings.general.FilesScreen
import com.example.settings.general.HelpScreen
import com.example.settings.general.PrivacyScreen
import com.example.settings.general.SavedChatsScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.example.chat.ui.DiagnosticScreen
import com.example.BuildConfig

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle response
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        // Check Firebase config (logged as warnings, non-blocking)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                android.util.Log.w("MainActivity", "Firebase configuration missing or invalid. Check google-services.json.")
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Firebase initialization log: ${e.message}")
        }

        // Get dependencies container
        val appContainer = (application as NabihApplication).container
        
        // Initialize view models using custom factory
        val factory = ViewModelFactory(appContainer)
        val homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        val chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
        val settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val layoutDirection = if (settings.language == AppLanguage.ARABIC) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            // Dynamically provide Language Alignment (RTL / LTR) across all layouts
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                val darkTheme = when (settings.theme) {
                    com.example.models.AppTheme.DARK -> true
                    com.example.models.AppTheme.LIGHT -> false
                    com.example.models.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                NabihTheme(darkTheme = darkTheme, isArabic = settings.language == AppLanguage.ARABIC) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = if (settings.isLoggedIn) "home" else "login"
                        ) {
                            // Onboarding and Premium Sign-in
                            composable("login") {
                                LoginScreen(
                                    settingsViewModel = settingsViewModel,
                                    onLoginSuccess = {
                                        chatViewModel.resetChatState()
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // Home Screen Routing
                            composable("home") {
                                MainScreen(
                                    homeViewModel = homeViewModel,
                                    chatViewModel = chatViewModel,
                                    settingsViewModel = settingsViewModel,
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onNavigateToRoute = {
                                        navController.navigate(it)
                                    }
                                )
                            }

                            // Settings Preferences Routing
                            composable("settings") {
                                SettingsScreen(
                                    settingsViewModel = settingsViewModel,

                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("saved") {
                                SavedChatsScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("files") {
                                FilesScreen(
                                    chatViewModel = chatViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    isArabic = settings.language == AppLanguage.ARABIC
                                )
                            }
                            composable("account") {
                                AccountScreen(
                                    settingsViewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    isArabic = settings.language == AppLanguage.ARABIC,
                                    onLogout = {
                                        chatViewModel.resetChatState()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onDeleteAccount = {
                                        settingsViewModel.viewModelScope.launch {
                                            appContainer.chatRepository.deleteAllConversations()
                                            appContainer.memoryRepository.deleteAllMemories()
                                            settingsViewModel.logout()
                                            settingsViewModel.saveApiKeys("", "", "", "")
                                            chatViewModel.resetChatState()
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                            composable("privacy") {
                                PrivacyScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("help") {
                                HelpScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                        }
                    }
                }
            }
        }
    }
}
