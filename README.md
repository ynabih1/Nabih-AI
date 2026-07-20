# Nabih AI - Android Application

Nabih AI is a modern, fast, and feature-rich AI assistant Android application. It supports seamless integration with various leading AI models, providing users with an intuitive interface for chatting, image generation, and managing conversations.

## Key Features

- **Multi-Model Support:** Chat seamlessly with Nabih Ultra (flagship native model), Google Gemini, OpenAI ChatGPT, and Anthropic Claude.
- **Model Switcher:** Dynamically switch between connected AI models from the chat screen.
- **Authentication:** Securely log in using Email/Password or Google Sign-In with full Arabic/English localized error handling.
- **Rich Media Chat:** Support for text, images, and document attachments within conversations.
- **Offline History:** All conversations are stored locally using Room Database for offline access.
- **Adaptive UI:** Fully localized in English and Arabic, with dynamic Light/Dark themes using a warm, minimalist color palette.
- **Downloadable Fonts:** Uses Google Fonts API (Source Serif 4, Inter, IBM Plex Sans Arabic) for beautiful, lightweight typography.

## Project Structure

The project has been reorganized into a feature-based structure for better maintainability:

```text
com.example/
├── auth/            # Authentication screens (Login, Account, Google Sign-In logic)
├── chat/            # Chat interface, feedback sheet, model switcher, and diagnostics
├── data/            
│   ├── local/       # Room Database (Entities, DAOs, AppDatabase)
│   ├── remote/      # API Services, Network clients, and AiProvider routing
│   └── repository/  # Repositories for Chat, Settings, and Memory
├── di/              # Dependency Injection (ViewModelFactory, AppContainer)
├── model/           # Core data models (AiModel, ApiProvider, Settings, Models)
├── settings/        # Settings screens, API Keys configuration, App features
├── ui/              
│   ├── components/  # Reusable UI elements (MarkdownRenderer, TypingAnimation)
│   └── theme/       # App Theme (Color, Typography, Theme)
├── utils/           # Helper utilities (NetworkMonitor, SecureStorage, DocumentParser)
├── MainActivity.kt  # Main entry point and Navigation Host
└── NabihApplication.kt # Application class
```

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle files.
4. Provide a valid `google-services.json` in the `app/` directory (for Firebase Auth/Google Sign-In).
5. Build and run the application.

## API Keys Configuration

Users can add their own API keys via the **Settings -> API Keys** screen in the app. The app securely stores these keys using EncryptedSharedPreferences and uses `AiRouter` to dynamically route requests to the correct provider.
