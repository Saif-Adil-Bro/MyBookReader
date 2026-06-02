# 📚 My Reader – Professional E-Book Reading App

A modern, production-ready Android e-book reading app built with **Jetpack Compose**, **Firebase**, and **Clean Architecture**.

---

## ✨ Features

### 🔐 Authentication
- Email/Password sign up & sign in with verification
- Google Sign-In via Credential Manager
- Anonymous guest access with later account upgrade
- Password reset via email

### 🏠 Home Screen
- Auto-scrolling featured book banner (3.5s interval)
- 9 category chips with icons
- Popular, Newly Added, Top Rated, Most Downloaded sections
- Continue Reading section for in-progress books

### 📖 Reading Experience
- **PDF viewer** – native `android-pdf-viewer` with pinch-to-zoom
- **EPUB viewer** – WebView-based with CSS injection for theming
- **3 reading modes**: Light / Dark / Sepia
- Font size (10–30pt) and line height (1.0–2.5x) controls
- Reading progress bar with page slider
- Page jump dialog
- **Bookmarks** panel (add, view, jump to, delete)
- Auto-save progress every 500ms

### 📥 Offline Reading
- Background downloads via WorkManager
- Real-time download progress (0–100%)
- Books stored in `app/files/books/`
- Cached in Room for offline library browsing

### 📚 Library
- **Favorites** tab – one-tap add/remove
- **Downloads** tab – offline books with reading progress bar
- **Recently Read** tab – auto-tracked history

### 👤 Profile & Analytics
- Avatar, name, email, membership badge
- Reading streak & longest streak
- Weekly bar chart (reading minutes per day)
- Monthly books chart
- Reading goals (daily minutes, weekly books)
- Achievement badge system (6 default achievements)
- Dark mode toggle

### 🔔 Push Notifications
- Firebase Cloud Messaging service
- Notification channels: New Books, Reading Reminders, General
- Deep links to book detail from notification tap
- Scheduled daily reminder at 6 PM (Cloud Function)
- New book auto-notification on Firestore create

### 🛡️ Admin Panel
- Overview stats (total books, users, downloads, new users today)
- Book management (add, edit, delete)
- Add Book form with cover image + file upload
- Book request management (approve/reject)
- Push notification sender with quick templates
- User management with search & ban

### 🏆 Smart Features
- Reading streak tracking (WorkManager daily check)
- Achievement unlocking system
- Background data sync (WorkManager 15-min intervals)
- Offline-first with Firestore local persistence
- Auto sync across devices via Firebase

---

## 🏗️ Architecture

```
app/
├── core/
│   ├── di/          # Hilt modules (Firebase, DB, Network, Repos)
│   ├── notification/ # FCM service
│   ├── utils/       # PreferencesManager, NetworkMonitor, Extensions
│   └── worker/      # StreakWorker, SyncWorker, BookDownloadWorker
├── data/
│   ├── local/
│   │   ├── dao/     # Room DAOs (Book, Download, Progress, Bookmark, History)
│   │   ├── database/ # MyReaderDatabase
│   │   └── entity/  # Room entities
│   └── repository/  # Implementations of domain interfaces
├── domain/
│   ├── model/       # Book, User, Bookmark, Achievement, etc.
│   ├── repository/  # Interfaces
│   └── usecase/     # (extend as needed)
└── presentation/
    ├── admin/       # Dashboard, AddBook, UsersManagement
    ├── auth/        # Login, Register, ForgotPassword, Onboarding
    ├── bookdetail/  # BookDetail
    ├── components/  # Reusable composables
    ├── home/        # Home, Search, Category, BookRequest
    ├── library/     # Library (Favorites/Downloads/Recent)
    ├── navigation/  # NavGraph, Screen routes
    ├── profile/     # Profile, ReadingAnalytics
    ├── reader/      # ReaderScreen, ReaderViewModel
    └── theme/       # Color, Typography, Theme
```

**Pattern:** MVVM + Clean Architecture + Repository pattern  
**DI:** Hilt  
**Local DB:** Room (offline-first)  
**Remote:** Firebase Firestore + Storage + Auth + FCM  
**Async:** Kotlin Coroutines + Flow  
**UI:** Jetpack Compose + Material Design 3

---

## 🚀 Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Firebase account

### 2. Clone & open
```bash
git clone <repo-url>
cd MyReader
# Open in Android Studio
```

### 3. Firebase setup
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project named **"MyReader"**
3. Add an **Android app** with package `com.myreader.app`
4. Download `google-services.json` and place it in `app/`
5. Enable these Firebase services:
   - **Authentication** → Email/Password + Google
   - **Firestore** → Start in production mode
   - **Storage** → Start in production mode
   - **Cloud Messaging**
   - **Analytics**
   - **Crashlytics**

### 4. Firestore indexes
```bash
firebase login
firebase deploy --only firestore:indexes
```

### 5. Firestore security rules
```bash
firebase deploy --only firestore:rules
firebase deploy --only storage:rules
```

### 6. Google Sign-In
1. In Firebase Console → Authentication → Sign-in method → Google → Enable
2. Get your **Web Client ID** from Firebase Console
3. Add to `app/src/main/res/values/strings.xml`:
```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
```

### 7. Cloud Functions (optional but recommended)
```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

### 8. Font files
Place these font files in `app/src/main/res/font/`:
- `nunito_light.ttf`
- `nunito_regular.ttf`
- `nunito_medium.ttf`
- `nunito_semibold.ttf`
- `nunito_bold.ttf`
- `nunito_extrabold.ttf`
- `playfair_regular.ttf`
- `playfair_medium.ttf`
- `playfair_bold.ttf`

Download from [Google Fonts](https://fonts.google.com)

### 9. Build & Run
```bash
./gradlew assembleDebug
# or run directly from Android Studio
```

---

## 🗃️ Firestore Data Structure

```
/books/{bookId}
  title, author, description, coverUrl, fileUrl, fileFormat,
  fileSizeBytes, totalPages, category, language, rating,
  ratingCount, downloadCount, isFeatured, isActive, createdAt

  /ratings/{userId}
    rating, userId

/users/{userId}
  displayName, email, photoUrl, role, isGuest,
  totalBooksRead, totalReadingMinutes, readingStreak,
  dailyReadingGoalMinutes, weeklyReadingGoalBooks, fcmToken

  /favorites/{bookId}
    bookId, addedAt

  /progress/{bookId}
    currentPage, totalPages, lastReadAt, totalReadingMinutes

  /bookmarks/{bookmarkId}
    bookId, page, label, note, highlightedText, highlightColor

/book_requests/{requestId}
  userId, bookTitle, authorName, description, status, createdAt

/reports/{reportId}
  bookId, userId, reason, createdAt

/admin_notifications/{notifId}
  title, body, targetUserIds, createdAt
```

---

## 🔒 Security

- Firestore rules: public book reads, owner-only user writes, admin-only book mutations
- Storage rules: authenticated reads for book files, size + type validation
- ProGuard enabled in release builds
- Firebase App Check (Play Integrity) configured
- Sensitive data never logged in release
- Guest accounts can only see their own data

---

## 🌐 Multi-language Support

Supported languages:
- **English** (default)
- **বাংলা (Bengali)**

Book content language filtering is built into the `BookLanguage` enum. To add more UI translations, add `res/values-bn/strings.xml` with Bengali translations.

---

## 📦 Key Dependencies

| Library | Purpose |
|---|---|
| Jetpack Compose + Material3 | UI framework |
| Hilt | Dependency injection |
| Room | Local SQLite database |
| Firebase (Auth, Firestore, Storage, FCM) | Backend |
| WorkManager | Background tasks |
| Paging 3 | Large list pagination |
| Coil | Image loading |
| android-pdf-viewer | PDF rendering |
| OkHttp | HTTP downloads |
| DataStore Preferences | User settings |
| Lottie | Animations |
| Timber | Logging |

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## 📄 License

Copyright © 2024 My Reader. All rights reserved.
# MyBookReader
