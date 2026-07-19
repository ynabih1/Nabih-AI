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

## إعداد مفتاح API قبل البناء اليدوي (Manual Builds Setup)

عند بناء التطبيق كـ **Release APK** أو تشغيله يدوياً من خلال **Android Studio** (بدلاً من بيئة AI Studio مباشرة)، ستحتاج إلى تكوين مفتاح Gemini API الحقيقي الخاص بك محلياً لضمان عمل طراز Nabih Ultra ومميزات Gemini بشكل طبيعي. الاعتماد على لوحة أسرار AI Studio (Secrets Panel) يعمل فقط داخل بيئة التطوير السحابية ولا ينطبق على البناء اليدوي المحلي.

لإعداد المفتاح، اتبع الخطوات التالية قبل تشغيل أي عملية Build أو Generate Signed APK:

1. **إنشاء ملف البيئة محلياً:**
   قم بنسخ ملف `.env.example` في الجذر الرئيسي للمشروع وأعد تسميته إلى `.env`.
   ```bash
   cp .env.example .env
   ```

2. **تحديث قيمة المفتاح:**
   افتح ملف `.env` وقم بتغيير قيمة `GEMINI_API_KEY` الوهمية بمفتاحك الحقيقي:
   ```properties
   GEMINI_API_KEY=AIzaSyYourActualKeyHere...
   ```

> ⚠️ **ملاحظة أمنية:** تم إضافة فحص وقت البناء (Build-time validation) يمنع تصدير APK أو إتمام عملية البناء في حال كانت قيمة المفتاح فارغة أو مطابقة للقيمة الوهمية `MY_GEMINI_API_KEY` لمنع تصدير تطبيقات غير صالحة بصمت. كما أن ملف `.env` مستثنى تماماً من تتبع Git عبر ملف `.gitignore` لحماية خصوصية مفاتيحك.

---

### Manual API Key Setup (English)

When building the application as a **Release APK** or running it directly via **Android Studio** (instead of the AI Studio cloud environment), you must configure your Gemini API Key locally. Relying on the AI Studio secrets panel only works within the cloud environment and does not apply to manual local builds.

To set up the key, follow these steps before executing any Build or Generate Signed APK task:

1. **Create the Environment File:**
   Copy the `.env.example` file in the project root and rename it to `.env`:
   ```bash
   cp .env.example .env
   ```

2. **Update the API Key:**
   Open `.env` and replace the placeholder `GEMINI_API_KEY` with your actual Gemini API key:
   ```properties
   GEMINI_API_KEY=AIzaSyYourActualKeyHere...
   ```

> ⚠️ **Security Note:** A build-time validation check is in place. It will halt the build with an error if the key is empty, contains placeholder words, or remains set to `MY_GEMINI_API_KEY`. The `.env` file is fully ignored via `.gitignore` to keep your credentials secure.

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
