# Bakaláři Android App

A native Android application for the Bakaláři school system API v3.

## Features

- **Login**: Authenticate with your school's Bakaláři instance using username/password
- **Subjects List**: View all subjects with their grade averages (color-coded by performance)
- **Marks Detail**: Click any subject to see all marks with details (date, description, weight, etc.)
- **Pull-to-refresh**: Swipe down to refresh marks data
- **Logout**: Securely log out and clear stored tokens

## Project Structure

```
app/src/main/java/com/example/bakalariapp/
├── data/
│   ├── api/
│   │   ├── BakalariApiService.kt    # Retrofit API interface
│   │   └── RetrofitClient.kt          # Retrofit configuration
│   ├── model/
│   │   ├── LoginResponse.kt           # Login API models
│   │   ├── MarksResponse.kt           # Marks/subjects API models
│   │   └── UserInfo.kt                # User API models
│   ├── preferences/
│   │   └── TokenDataStore.kt          # Encrypted token storage
│   └── repository/
│       └── BakalariRepository.kt      # Data repository layer
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt                # Jetpack Navigation setup
│   ├── screens/
│   │   ├── LoginScreen.kt             # Login UI
│   │   ├── SubjectsScreen.kt          # Subjects list UI
│   │   └── MarksDetailScreen.kt       # Marks detail UI
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel/
│       ├── LoginViewModel.kt
│       └── MarksViewModel.kt
└── MainActivity.kt
```

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with ViewModels
- **Networking**: Retrofit2 + OkHttp + Gson
- **Navigation**: Jetpack Navigation Compose
- **Storage**: DataStore Preferences (for tokens)
- **Async**: Kotlin Coroutines + Flow

## How to Build

1. Open the project in Android Studio
2. Sync Gradle files
3. Run on an emulator or device (minSdk: 26, targetSdk: 34)

## API Documentation

Based on the [Bakaláři API v3](https://github.com/bakalari-api/bakalari-api-v3) documentation:

- **Login**: `POST /api/login` with `client_id=ANDR&grant_type=password`
- **Get Marks**: `GET /api/3/marks` with `Authorization: Bearer {token}`

## Usage

1. Enter your school's Bakaláři URL (e.g., `https://your-school.cz`)
2. Enter your username and password
3. View your subjects with averages
4. Tap any subject to see all marks with details

## Notes

- The app stores the access token securely using DataStore
- Supports both grade-based (1-5) and points-based grading
- Grade colors: Green (1.x), Light Green (2.x), Yellow (3.x), Orange (4.x), Red (5.x)
