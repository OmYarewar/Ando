# Ando 📱

An elegant, modern, offline-first Android application designed to interface with the **NVIDIA NIM API**, structured as a personal AI workspace with dynamic Local Retrieval-Augmented Generation (RAG) and SQLite-powered memory/skill context-injection.

Built with **Jetpack Compose**, **Room Database (SQLite)**, and styled natively with **Material Design 3**.

---

## 🚀 Key Features

*   **Ando Chat Terminal**: An interactive messaging client optimized for speed and fluidity, featuring dynamic status indicators and streaming UI feel powered by NVIDIA NIM endpoints.
*   **Local RAG Memory Bank**: In-app SQLite retrieval engine that automatically indexes key facts from conversation strings and inserts relevant prompt expansions in real time.
*   **AI Workspace & Skills Folder**: Customizable system prompts and persona cards. Save expert custom instruction sets (e.g. Code Optimizers, Creative Writers) and toggle them dynamically to inject into the active NIM orchestration.
*   **Web Search Synchronization**: A fast query-augmenting search routine that enhances conversation accuracy with up-to-date web intelligence.
*   **Fully Offline-First Metadata**: All conversations, extracted memories, and customized systems are tracked locally with Room persistence.

---

## 🛠️ Tech Stack & Architecture

This repository adopts modern Android practices matching native application architecture:

*   **Language**: 100% [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material Design 3 (M3)
*   **Database**: [Room SQLite](https://developer.android.com/training/data-storage/room) for local persistence and index searches
*   **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & OkHttp3 for clean type-safe API requests
*   **Architecture Pattern**: Clean Architecture + MVVM (Model-View-ViewModel) + unidirectional state flow (UDF)
*   **Asynchronous Engine**: Kotlin Coroutines and StateFlow for non-blocking asynchronous state collections
*   **Build System**: Gradle Kotlin DSL (`.build.gradle.kts`)

---

## 📦 Getting Started & Installation

### Prerequisite: NVIDIA NIM Key
To power the AI dialogue, obtain a free API Token from the [NVIDIA API Catalog](https://build.nvidia.com/) and input it securely on the **AI Workspace & Credentials** page within the app.

### Manual Build Instructions
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/ando.git
   cd ando
   ```
2. Open the project inside **Android Studio** (Ladybug or newer).
3. Let Gradle sync and download relevant dependencies.
4. Hit **Run** or build the debug assembly via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
The compiled APK will be outputted under:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚙️ AI Studio Platform Features
Since this project is optimized for the **Google AI Studio platform**, you can:
*   **Stream Live previews** instantly using the browser emulator.
*   **Generate Fresh APKs** directly from the sidebar settings.
*   **Deploy directly to GitHub** or sync changes via the integration sidebar.

---

## 📂 Project Structure

```
ando/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/             # Room Entity schemas, DAOs, and database connectors
│   │   ├── repository/       # Data syncing and network accessors
│   │   ├── ui/               # Complete Composable UX Screens, components, and Material themes
│   │   └── viewmodel/        # ChatViewModel orchestrating screen UI States
│   └── build.gradle.kts      # Module level dependencies configuration
├── metadata.json             # AI Studio Platform Metadata configuration
└── build.gradle.kts          # Root-level build configuration scripts
```

---

*Enjoy a clean, fast, and highly contextual local AI workspace experience with Ando!*
