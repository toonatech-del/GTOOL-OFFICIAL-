# Play Store Data Safety Notes — GTOOL X

This document provides a factual breakdown of data access, network usage, and privacy practices in **GTOOL X** to assist with Google Play Console Data Safety form declarations.

---

## 1. Overview
- **Developer Name**: GSD
- **Support Email**: hgdduf93@gmail.com
- **Account Requirement**: None (No user accounts or sign-in required).
- **Advertising**: None (No AdMob or third-party ad networks).
- **Analytics & Tracking**: None (No Firebase Analytics, Crashlytics, or tracking SDKs).
- **Developer Servers**: None (Developer operates no cloud storage or backend API servers for user content).

---

## 2. Data Types & Processing

| Data Type | Accessed | Collected / Transferred Off Device | Stored Locally | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Files & Documents** (PDFs, Images, Notes) | Yes (User Selected) | No | Yes (Encrypted/Local Vault) | App Functionality (Viewing, OCR, Resizing, Invoice generation) |
| **Camera** | Yes | No | Yes (When saved) | App Functionality (Document scanning & photo capture) |
| **Biometric Data** | Yes (Device API) | No | Device Hardware Only | Authentication / App Lock (Managed entirely by Android OS) |
| **Notifications & Alarms** | Yes | No | Yes | App Functionality (Local task reminders) |

---

## 3. Network & Internet Access
- **Google Play Services Document Scanner**: The app uses `com.google.android.gms:play-services-mlkit-document-scanner`. Play Services on the device may communicate with Google infrastructure to download ML model modules or updates.
- **Local OCR**: ML Kit Text Recognition operates locally on device.

---

## 4. Security & Protection
- **Storage**: User documents, notes, and records are stored locally on the device inside app-isolated private directories (`noBackupFilesDir`).
- **Biometric Security**: Biometric authentication uses Jetpack Biometric Prompt backed by Android KeyStore.
- **Backup & Export**: User-initiated ZIP exports are handled locally on device via Android Storage Access Framework.

---

## 5. Summary for Play Console
- **Data Collected**: None by developer.
- **Data Shared**: None shared with third parties by the app (except when explicitly exported/shared by user via Android Share Sheet).
- **Security Practices**: Data stored locally in private storage; device biometric lock support; user can delete all local data at any time.
