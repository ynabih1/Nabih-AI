# Nabih AI

Nabih AI is a professional, high-performance Android AI assistant built using Kotlin, Jetpack Compose, and modern Android architecture. Designed with a clean Material 3 user interface, the application offers a cohesive, highly responsive experience with native support for both English and Arabic languages.

## Project Description

Nabih AI functions as a central portal for communicating with advanced artificial intelligence models. It provides users with direct, configurable access to multiple leading AI engines, including Gemini, ChatGPT, Claude, and specialized Nabih models. The application emphasizes local control, allowing users to safely manage and input their own API credentials directly on-device.

## Core Features

- **Multi-Model Support:** Configure and switch seamlessly between Gemini, ChatGPT, Claude, and Nabih Ultra models.
- **Material 3 Design:** Built entirely with Jetpack Compose following Material 3 guidelines, featuring beautiful, eye-safe dark layouts, smooth transitions, and dynamic components.
- **Bilingual Experience:** Full localized interface and navigation support for both English and Arabic languages.
- **Voice & Chat Assistant:** Responsive, low-latency text and voice interactions with conversational memory retention.
- **Secure Key Storage:** Local settings architecture that securely handles and stores user API keys on-device without exposing them.
- **Clean Architecture & MVVM:** Structured following modern Android practices using repositories, custom ViewModels, Room database for local state persistence, and Kotlin Coroutines/Flows.

## Installation

1. **Prerequisites:**
   - Android Studio (Ladybug or newer)
   - Android SDK 34+
   - Gradle 8.0+

2. **Clone the Repository:**
   ```bash
   git clone https://github.com/ynabihx/nabih-ai.git
   cd nabih-ai
   ```

3. **Configure API Keys:**
   - Define credentials in the secure **Settings** screen inside the application.
   - Alternatively, copy `.env.example` to `.env` and specify default keys.

4. **Build and Run:**
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Run the `:app` module on a compatible Android device or emulator.

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
