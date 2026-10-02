# GTOOL X

### On-Device Document & Productivity Utilities for Android

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge&logo=android)](https://www.android.com/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-Local_Processing-00C853.svg?style=for-the-badge&logo=shield)](https://github.com)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg?style=for-the-badge&logo=githubactions)](https://github.com)

---

## 📱 Features & Capabilities

GTOOL X provides essential daily productivity, document management, and photo utility features on Android:

- **PDF Suite**: Local PDF previewing, page rendering, text extraction, merging, splitting, and compression.
- **Photo Resizer & Dimension Helpers**: Crop and resize photos with standard size and DPI presets for application forms, passport photos, and ID cards.
- **Local OCR & Document Scanner**: Extract selectable text from scanned images and bills using Google Play Services Document Scanner & local ML Kit text recognition.
- **Notes & Universal Search**: Store notes and perform indexed FTS4 full-text searches across local documents.
- **Smart Invoice Maker**: Generate clean, itemized invoices and bills with automatic totals calculation.
- **Task Reminders**: Schedule exact local alarms and notifications for due dates and bills.
- **Biometric Lock**: Protect app access with Android fingerprint or face unlock.
- **Local Backup & Restore**: Export and import backup archives locally using Android's safe file picker.

---

## 🛡️ Privacy, Security & Disclaimers

### Local Data Processing
- User documents, notes, OCR results, and generated files are processed and stored locally on the device inside app-isolated private storage (`noBackupFilesDir`).
- Developer operates no cloud storage servers or user accounts.

### Dependency & Network Information
- **Google Play Services Document Scanner**: The application utilizes `com.google.android.gms:play-services-mlkit-document-scanner`. On-device Google Play Services may download model updates or modules from Google infrastructure.

### Legal Disclaimers
- **Photo Resizer Presets**: Preset dimensions are general size helpers. Users are responsible for verifying final specifications against official form guidelines.
- **Invoice & Tax Disclaimer**: Invoice helper calculations are provided for convenience. Users are responsible for verifying tax rates, invoice details, and legal compliance.
- **OCR Accuracy**: OCR document extraction accuracy depends on input image quality. Users should verify extracted text for critical uses.

---

## 📄 Licensing & Open Source Notices
See [THIRD_PARTY_NOTICES.txt](./THIRD_PARTY_NOTICES.txt) for open-source library licenses and dependency notices.

---

## 📱 App Previews

<p align="center">
  <img src="./Screenshot_20260930-173249.png" width="45%" alt="Dashboard & Tools" />
  <img src="./Screenshot_20260930-173313.png" width="45%" alt="Offline Search" />
</p>

<p align="center">
  <img src="./Screenshot_20260930-173255.png" width="45%" alt="Security & Settings" />
</p>
