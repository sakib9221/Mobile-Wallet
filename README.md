# Mobile Wallet 📱🚀
An elegant, modern, offline-first Personal Finance & Debt Tracker built with native **Kotlin** and **Jetpack Compose** using the **Material Design 3** specification. Designed for fluid performance, dynamic animations, and local-first data resilience.

---

## 🌟 Key Features

### 1. Unified Dashboard & Expense Tracking 📊
* **Income & Expense Logging:** Categorize your day-to-day transactions effortlessly.
* **Category Breakdown:** Beautiful, dynamic visuals outlining your food, shopping, utilities, and lifestyle expenses.
* **Smooth Scrolling List:** Optimized with custom interaction physics, explicit list-item content type keying, and lightweight rendering to eliminate latency.

### 2. Smart Backup & Auto-Restore System 💾
* **No More Data Loss:** The wallet automatically triggers continuous local backups to your public standard `Downloads` directory under `personal_finance_backup.json`.
* **Smarter Auto-Restore on Install:** On fresh installs or app launches, it auto-detects existing downloads and selects the backup containing the largest record count to perform a completely automated restore.
* **clutter Free Output:** Includes smart deduplication algorithms that safely purge duplicate files like `personal_finance_backup (1).json` to prevent digital clutter.

### 3. Integrated Debts & Settle System 🤝
* **Directional Ledger:** Monitor what you owe (**Payable**) vs. what you are owed (**Receivable**).
* **Fractional/Partial Settling:** Settle payments sequentially by typing a specified partial amount, or clear debts entirely with a single click.

### 4. Gorgeous Dual-Language UI 🇧🇩🇺🇸
* Toggle between **বাংলা (Bengali)** and **English** on-the-fly dynamically.
* All prompts, menus, settings, and updates are properly localized into rich native Bengali.

### 5. Custom Thems & Custom Transitions 🎨
* Choose between beautiful system themes: **Light**, **Dark**, **Slate**, and **Cosmic Slate**.
* Features customized high-response spring scale/alpha clicks and fluid tactile transitions on interactions.

### 6. Dynamic In-App Update Engine 🔄
* Clean Material 3 alert dialogue verifying updates gracefully.
* Fully responsive layout using optimal viewport padding with **Update Now** & **Maybe Later** buttons that auto-scale perfectly on smaller devices.

---

## 🛠️ Architecture & Tech Stack

This application is built adhering to Google's official Android Architecture Guidelines (MVVM + Clean Architecture principles):

* **Language:** 100% Native Kotlin
* **UI Framework:** Jetpack Compose (Declarative UI)
* **Design System:** Material Design 3 (M3)
* **Dependency & State Management:** Android Jetpack ViewModel & StateFlow
* **Database (Local Persistence):** Room Database API for efficient client-side SQLite storage
* **Build System:** Gradle (Kotlin DSL - `build.gradle.kts`) with resiliant Gradle-controlled dynamic build APK renaming configurations.

---

## 🚀 How to Run Manually & Control Version Codes

### 1. Changing Version Code & Name
If you are looking to update your version info manually:
1. Open the file `app/build.gradle.kts` in your project explorer.
2. Locate the `defaultConfig` block (typically around lines 13–18):
   ```kotlin
   android {
       namespace = "com.example"
       defaultConfig {
           applicationId = "com.aistudio.financetracker.vqyhm"
           minSdk = 24
           targetSdk = 36
           versionCode = 2       // increment this integer manually (e.g., 3, 4, etc.)
           versionName = "2.0"   // change this string for display updates (e.g., "2.1")
       }
   }
   ```
3. Sync Project with Gradle Files & Rebuild!

### 2. Output APK Configuration
By design, the build system automatically intercepts compiling outputs to output naming structure, dropping outputs beautifully inside your workspace folder as `Mobile.Wallet.v2.0.apk` instead of generic outputs.

---

## 📂 Project Structure

```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── java/com/example
│   │   │   │   ├── data/       # Room Database Entities, DAOs, & Core Repository
│   │   │   │   └── ui/         # Jetpack Compose Screens, ViewModels & Visual Styles
│   │   │   └── res/            # Static Localizable Strings & App Icons
│   └── build.gradle.kts        # Individual app dependencies & build settings
├── build.gradle.kts            # Project-level configurations
└── settings.gradle.kts         # Module configurations
```

---

## 🔐 Security & Privacy
Because **Mobile Wallet** is localized completely on-device, all inputs, records, and calculations processed locally will never leave your phone. Direct database modifications are protected under Android's secure application sandbox.
