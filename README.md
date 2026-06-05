# BoxBox — F1 Companion App
## Android Studio Setup Guide

---

## 1. Create Project in Android Studio

1. Open Android Studio → **New Project**
2. Select **Empty Activity (Compose)**
3. Name: `BoxBox`
4. Package: `com.boxbox.app`
5. Minimum SDK: **API 26 (Android 8.0)**
6. Language: **Kotlin**
7. Click **Finish**

---

## 2. Copy Project Files

Replace the generated files with the provided ones:

```
app/build.gradle.kts          ← replace
app/src/main/AndroidManifest.xml ← replace

app/src/main/java/com/boxbox/app/
├── BoxBoxApp.kt
├── MainActivity.kt
├── data/
│   ├── api/
│   │   ├── ApiInterfaces.kt
│   │   └── RetrofitClient.kt
│   ├── model/
│   │   └── Models.kt
│   └── repository/
│       └── BoxBoxRepository.kt
├── viewmodel/
│   ├── HomeViewModel.kt
│   ├── LiveViewModel.kt
│   ├── StandingsViewModel.kt
│   └── ProfileViewModel.kt
├── ui/
│   ├── theme/
│   │   └── Theme.kt
│   ├── Components.kt
│   ├── home/HomeScreen.kt
│   ├── live/LiveScreen.kt
│   ├── standings/StandingsScreen.kt
│   ├── results/ResultsScreen.kt
│   └── profile/ProfileScreen.kt
└── notifications/
    ├── NotificationService.kt
    └── RaceNotificationScheduler.kt

app/src/main/res/
├── values/themes.xml
└── xml/file_paths.xml
```

---

## 3. Firebase Setup

### 3.1 Create Firebase Project
1. Go to **console.firebase.google.com**
2. Click **Add project** → name it `BoxBox`
3. Disable Google Analytics (optional)

### 3.2 Add Android App
1. In Firebase Console → **Add app** → Android
2. Package name: `com.boxbox.app`
3. Download **google-services.json**
4. Place it in: `app/google-services.json`

### 3.3 Enable Firebase Services
In Firebase Console enable:
- **Authentication** → Email/Password
- **Firestore Database** → Start in test mode
- **Storage** → Start in test mode
- **Cloud Messaging** (already enabled by default)

### 3.4 Firestore Rules (for testing)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 3.5 Storage Rules
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profile_photos/{userId}.jpg {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 4. Add FileProvider to AndroidManifest

Add inside `<application>` tag:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

---

## 5. Sync & Build

1. Click **Sync Now** after pasting build.gradle.kts
2. Wait for Gradle sync to complete
3. Click **Run** (green play button)

---

## 6. App Features

| Screen | Features |
|--------|----------|
| 🏠 Home | Race calendar, countdown timer, session schedule, notification button |
| 🏎️ Live | Track map with car dots, timing board, race control feed |
| 🏆 Standings | Driver & constructor standings toggle |
| 📋 Results | Past race results |
| 👤 Profile | Sign in/up, edit profile, camera photo, CRUD, notifications |

## CRUD Coverage
- **Create** — Sign up creates Firebase Auth + Firestore document
- **Read** — Load profile from Firestore
- **Update** — Edit name, team, favourite driver, notifications, photo
- **Delete** — Delete account removes Firestore doc + Storage photo + Auth

## APIs Used
- **OpenF1** — Live positions, race control, stints, laps, intervals
- **Jolpica** — Standings, schedule, results

## Android Features Used
- ✅ Web content (Retrofit API calls)
- ✅ Notifications (FCM + AlarmManager)
- ✅ Camera (profile photo)
- ✅ Coroutines (live polling every 3s)
- ✅ MVVM architecture
- ✅ Jetpack Compose
- ✅ Firebase (Auth, Firestore, Storage)

---

## Good luck! 🏎️🏁
