# Swizima Monorepo Project

Welcome to the **Swizima** multi-platform automotive management suite.

## 🏗️ Project Structure & Recent Reorganization

The project has been reorganized into a unified workspace under the `swizima-app` directory with Git version control settings managed at the workspace root:

```text
swizima-app/
├── .git/                 # Git version control settings (repository root)
├── .gitignore            # Multi-project ignore configuration
├── swizima-mobile/       # Native Android Application (Jetpack Compose, Kotlin, Material 3)
├── swizima-api/          # Backend API services
└── swizima-web/          # Web frontend application
```

### Recent Changes & Reorganization
- **Workspace Reorganization:** Consolidated project structure into `swizima-app` containing mobile, web, and API subprojects.
- **Git Settings Relocation:** Moved the `.git` repository folder to `swizima-app/.git` so that all repository tracking and settings reside at the workspace level.
- **Enhanced `.gitignore`:** Reconfigured `.gitignore` to comprehensively ignore build outputs, caches (`.gradle`, `.idea`), local properties, node modules, and virtual environments across all subprojects (`swizima-mobile`, `swizima-api`, `swizima-web`).

---

## 📱 Subprojects

### 1. Swizima Mobile (`swizima-mobile`)
Native Android automotive management application built with Jetpack Compose and Material 3.
- **Features:** Authentication (Phone/Email with country codes and OTP), Automotive Dashboard (Car Park, Car Wash, Repair, Review), Service Records Management, PDF Receipt Generation, and WhatsApp Ticket Sender.
- **Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coroutines.

### 2. Swizima API (`swizima-api`)
- Backend services and database integration for syncing vehicle records, user authentication, and notifications.

### 3. Swizima Web (`swizima-web`)
- Administrative dashboard and web client for managing swizima automotive operations.

---

## 🚀 Getting Started

1. Clone or open the workspace at `swizima-app`.
2. Open `swizima-mobile` in Android Studio.
3. Configure API endpoints and dependencies as needed.
