# ChronoSync Task Engine

A native Android task management application built with **Jetpack Compose**, **MVVM architecture**, and **Room Database**, featuring a secure dual-factor login system and full data backup/import support.

---

## 🧭 Overview

ChronoSync Task Engine is a single-user, offline-first productivity app that lets users manage tasks, track productivity trends, and stay on top of approaching deadlines — all backed by a local Room database and protected by a PIN-based authentication flow.

---

## 🏗️ Architecture

The app follows the **MVVM (Model-View-ViewModel)** pattern with a hybrid navigation approach:

- **UI Layer** — Jetpack Compose screens (Fragment-hosted) for the dashboard, analytics, notifications, and settings
- **ViewModel Layer** — Manages business logic, filtering, and state via `StateFlow`
- **Repository Layer** — Single source of truth (`TaskFlowRepository`) abstracting Room DB + SharedPreferences
- **Data Layer** — Room database entities and DAOs

`MainActivity` uses a **hybrid architecture**: the navigation shell (bottom nav, theming) is built in Compose, while screen content is managed through Fragments using `.show()`/`.hide()` transactions for instant, "Steam-style" tab switching without reload.

---

## 📦 Module Breakdown

### `ui` package

#### `auth` — Authentication & Recovery
- **`AuthActivity.kt`** — Dual-factor security check on launch: verifies the `isLoggedIn` flag *and* confirms a user profile actually exists in Room. If the flag is true but the DB is empty (e.g. after a partial backup restore), the app auto-resets login state and redirects to auth.
    - **Account Recovery** — restore an existing account via Username + PIN
    - **Task Import** — new users can import tasks from a backup file while creating an account
    - Material 3 styled UI (`ElevatedCard`, `OutlinedTextField`, gradient `Scaffold`) with proactive field validation before file picker launches

#### `fragments` — Core App Screens
| Screen | Purpose |
|---|---|
| **MainScreenFragment** | Task dashboard — add/edit/delete/toggle tasks via a Modal Bottom Sheet (date & time picker), dynamic search, multi-level filters (Status, Date, Category) |
| **AnalyticsFragment** | Productivity insights — metric cards, Donut Chart (completion %), Bar Graph (category distribution) |
| **NotificationFragment** | Smart alerts with 3-tier urgency: 🔴 High (< 1.5h), 🟠 Medium (< 3h), 🟡 Low (< 6h) |
| **SettingsFragment** | Profile management (PIN-verified), JSON backup export, dark theme toggle, logout, wipe data |

### `utils` package
- **`BackupHelper.kt`** — Handles data portability
    - `backupToJson()` — serializes User + Tasks (preserving `createdAt`) into pretty-printed JSON
    - `restoreFromJson()` — deserializes safely with try-catch; falls back gracefully if older backups are missing fields (backward compatible)

### `viewmodel` package
| ViewModel | Responsibility |
|---|---|
| **TaskViewModel** | Search/filter pipeline, CRUD ops, "Out of Date" logic |
| **AuthViewModel** | Login/session lifecycle, backup-based account recovery, input validation |
| **AnalyticsViewModel** | Real-time productivity metrics & category breakdowns |
| **NotificationViewModel** | 60-second ticker for deadline re-evaluation, urgency sorting |
| **SettingsViewModel** | Profile updates, safe data wipe, backup generation |
| **ViewModelFactory** | Dependency injection for all ViewModels via shared repository |

### `model` package
- **Entities:** `Task.kt` (title, description, category, due date, completion, `createdAt`), `User.kt` (single hardcoded user, ID = 1, username + 4-digit PIN)
- **DAOs:** `TaskDao` (CRUD + real-time `Flow`), `UserDao` (profile ops, recovery lookup by username)
- **`TaskDatabase.kt`** — Singleton Room DB, currently **v2**, destructive migrations enabled
- **`TaskFlowRepository.kt`** — Single source of truth; wraps Room + SharedPreferences; provides atomic `clearAllUserData()` wipe

### `main` package
- **`TaskFlowApplication.kt`** — App entry point; initializes DB, SharedPreferences, and the global repository
- **`MainActivity.kt`** — Secure dashboard shell; strict dual-factor security gate before rendering UI; hybrid Compose + Fragment navigation; preserves tab state across configuration changes

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Fragment-hosted)
- **Architecture:** MVVM
- **Database:** Room
- **State Management:** StateFlow
- **Persistence:** SharedPreferences (session, theme, lock preferences)
- **Data Portability:** JSON-based backup & restore

---

## 🔒 Security Highlights

- 4-digit PIN-based authentication
- Dual-factor login check (flag + DB verification) to prevent stale-session access
- PIN re-verification required for sensitive profile edits and destructive actions

---

## 📋 Status

Actively in development as part of an Android development internship project.