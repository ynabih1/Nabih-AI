# Nabih AI - Unified Multi-Model AI Client

> **A unified, modern Android gateway for multi-model AI conversations powered by Gemini (via Firebase AI SDK), Claude, ChatGPT, and Nabih Ultra.**

---

## Overview

**Nabih AI** is a native Android application built with modern Kotlin and Jetpack Compose. It acts as an all-in-one interface allowing users to seamlessly interact with leading artificial intelligence models—including Google Gemini, Anthropic Claude, OpenAI GPT-4o, and Nabih AI's custom Ultra engine—from a single, unified interface with localized bilingual support.

---

## Key Features

- 🔐 **Firebase Authentication**: Seamless sign-in experience supporting Email/Password and Google Sign-In.
- ⚡ **Multi-Model Switching**: Instant toggling between Gemini 2.5 / 2.0 (via native Firebase AI SDK), Claude 3.5 Sonnet, GPT-4o, and Nabih Ultra.
- 🔑 **Encrypted Local Storage**: Secure client-side storage for custom API keys using `EncryptedSharedPreferences`.
- 🌍 **Full Bilingual Support**: First-class Arabic and English support with dynamic RTL / LTR layout mirroring.
- 🎨 **Adaptive Material 3 Design**: Custom warm light and dark themes tailored for comfortable long-session reading.
- 📂 **Offline-First Chat Persistence**: Local conversation history, folder organization, pinning, and archiving backed by Room Database.
- 📄 **Attachment & Document Parsing**: Support for uploading and processing images and text documents directly within chats.

---

## Project Structure

```
app/src/main/java/com/example/
├── auth/                       # Login, Account creation, Google Sign-In & Profile management
├── chat/
│   ├── ui/                     # Chat UI, Message bubbles, Composer, Feedback dialogs & Provider icons
│   └── logic/                  # ChatViewModel & HomeViewModel managing chat streams and folders
├── models/                     # AI Provider logic (Gemini, Claude, OpenAI), Model registry & Error translators
├── settings/
│   ├── apikeys/                # Screen for managing custom API keys securely
│   ├── general/                # Settings preferences, Saved Chats, Files, Privacy & Help screens
│   └── profile/                # SettingsViewModel for preferences & state management
├── data/
│   ├── local/                  # Room Database, Entities, DAOs & Migrations
│   └── repository/             # Repositories bridging local persistence and AI model providers
├── ui/
│   ├── theme/                  # Material 3 Color palette, Typography, and Theme definitions
│   └── components/             # Reusable Composables (Markdown renderer, Typing indicators)
├── utils/                      # Secure storage, Network monitor, Arabic post-processor, Document parser & Notifications
├── di/                         # AppContainer and ViewModelFactory for manual Dependency Injection
├── MainActivity.kt             # Navigation host and activity lifecycle entry point
└── NabihApplication.kt         # Application instance initializing global dependencies
```

---

## Tech Stack

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM with Repository Pattern & Manual Constructor Injection
- **AI Integration**: Firebase AI Logic SDK (Gemini), Retrofit2 / OkHttp3 (Claude & OpenAI REST APIs)
- **Authentication**: Firebase Auth & Google Identity Services
- **Local Database**: Room Database with Kotlin Symbol Processing (KSP)
- **Asynchrony**: Kotlin Coroutines, StateFlow, and SharedFlow
- **Image & Content**: Coil Compose

---

## Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: Version 17
- **Android SDK**: Compile SDK 36 (Minimum SDK 24)
- **Gradle**: 8.x (Kotlin DSL)

### Installation & Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/username/nabih-ai.git
   cd nabih-ai
   ```

2. **Configure Environment Variables**:
   Copy `.env.example` to `.env` in the root directory and add your keys:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   GOOGLE_CLIENT_ID=your_web_client_id.apps.googleusercontent.com
   ```

3. **Add Firebase Configuration**:
   Place your `google-services.json` file inside the `app/` directory. Ensure Firebase Authentication and Firebase AI Logic are enabled in your Firebase Console.

4. **Build & Run**:
   Build the debug APK using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

---

## License

Distributed under the **Apache License 2.0**. See [`LICENSE`](./LICENSE) for details.
