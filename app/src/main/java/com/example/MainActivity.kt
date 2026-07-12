package com.example

import com.example.core.di.ViewModelFactory
import com.example.core.model.AppLanguage
import com.example.core.model.AppTheme
import com.example.core.theme.NabihTheme
import com.example.feature.auth.AccountScreen

import com.example.feature.auth.LoginScreen
import com.example.feature.chat.ChatViewModel
import com.example.feature.chat.HomeViewModel
import com.example.feature.chat.MainScreen
import com.example.feature.chat.VoiceScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.tools.FilesScreen
import com.example.feature.tools.HelpScreen
import com.example.feature.tools.PrivacyScreen
import com.example.feature.tools.SavedChatsScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



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
                NabihTheme(darkTheme = settings.theme == com.example.core.model.AppTheme.DARK) {
                    Surface(modifier = Modifier.fillMaxSize()) {
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
                                    onNavigateToVoice = {
                                        navController.navigate("voice")
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
                                    onClearChatHistory = {
                                        settingsViewModel.viewModelScope.launch {
                                            appContainer.chatRepository.deleteAllConversations()
                                        }
                                    },

                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // Immersive Voice Call Mode
                            composable("voice") {
                                VoiceScreen(
                                    settingsViewModel = settingsViewModel,
                                    chatViewModel = chatViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("saved") {
                                SavedChatsScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("files") {
                                FilesScreen(onNavigateBack = { navController.popBackStack() }, isArabic = settings.language == AppLanguage.ARABIC)
                            }
                            composable("account") {
                                AccountScreen(
                                    settingsViewModel = settingsViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    isArabic = settings.language == AppLanguage.ARABIC,
                                    onLogout = {
                                        settingsViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    onDeleteAccount = {
                                        settingsViewModel.viewModelScope.launch {
                                            appContainer.chatRepository.deleteAllConversations()
                                            appContainer.memoryRepository.deleteAllMemories()
                                            settingsViewModel.logout()
                                            settingsViewModel.saveApiKeys("", "", "", "")
                                            navController.navigate("login") {
                                                popUpTo("home") { inclusive = true }
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
