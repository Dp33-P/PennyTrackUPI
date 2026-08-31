# PennyTrack UPI

Offline-first personal UPI and cash spend tracker for Android.

## What is included

- SMS inbox import for bank and UPI transaction messages.
- Notification listener for payment app and bank alerts.
- Fully local Room database.
- Built-in Indian merchant/category rules for food, grocery, medicine, rent, recharge, gas, EMI, insurance, subscriptions, fuel, travel, shopping, and utilities.
- Learned merchant records: when a new payee is found, it is saved locally and future spends can reuse the corrected category.
- Multi-parameter duplicate scoring using amount, time window, UPI ref/UTR, merchant, account hint, source, direction, and message similarity.
- Cash expense entry with quick amounts.
- Minimal Compose UI for dashboard, transactions, cash entry, review, and settings.
- No `INTERNET` permission.

## Build

Open this folder in Android Studio:

```text
C:\Users\u29c50\Documents\Codex\2026-08-24\i-n\outputs\PennyTrackUPI
```

Use Android Studio's bundled JDK 17 or newer. Let Android Studio sync Gradle, then run the `app` configuration on your Android phone.

## First run

1. Allow SMS access.
2. Tap `Scan` to import existing bank/UPI SMS.
3. Open notification access and enable `PennyTrack transaction detector`.
4. Set your monthly budget in `More`.

Everything stays on the phone.

## Build APK with GitHub Actions

1. Create a new GitHub repository.
2. Upload every file from this `PennyTrackUPI` folder.
3. Open the repository on GitHub.
4. Go to `Actions`.
5. Select `Build APK`.
6. Click `Run workflow`.
7. When it finishes, open the completed workflow run.
8. Download the `PennyTrackUPI-debug-apk` artifact.
9. Extract the zip. Your APK is inside as `app-debug.apk`.
