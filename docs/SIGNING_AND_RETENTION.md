# APK Upgrade Data Retention & Signing Invariant

## 1. Overview & Offline Guarantee

Kharcha is a 100% offline personal finance tracker with zero cloud sync, zero backend servers, and zero accounts. All ledger transactions, accounts, categories, budgets, recurring bills, udhar records, and attached receipts/documents reside entirely on the user's physical device.

When a user updates Kharcha by installing a new APK (or app update), the Android OS guarantees the preservation of all app-private data (`/data/data/com.sijan.kharcha/`) **if and only if** the following four invariants hold:

1. **Package Identity Continuity**: `applicationId` remains strictly `"com.sijan.kharcha"`.
2. **Signing Certificate Continuity**: The update APK is signed with the identical cryptographic key certificate as the currently installed APK.
3. **Version Code Monotonicity**: `versionCode` in `build.gradle.kts` strictly increments (`>= installed versionCode`).
4. **Non-Destructive Database Migrations**: Room database migrations are explicit; `fallbackToDestructiveMigration()` is strictly forbidden.

A violation of signing continuity causes Android's Package Manager to fail the update with:
```
INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.sijan.kharcha signatures do not match newer version
```
To recover from `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the user would be forced to uninstall Kharcha before installing the update. Because Kharcha is offline, **an uninstall wipes all database tables, encryption keys, and attached receipts**, causing irrecoverable data loss.

---

## 2. Release Signing Configuration

Release signing is configured in `app/build.gradle.kts`. It supports environment variables or Gradle project properties without hardcoding secrets in source control.

### Configuration Parameters

| Property / Environment Variable | Default / Fallback | Description |
|---------------------------------|--------------------|-------------|
| `KHARCHA_KEYSTORE_PATH` | `kharcha-release.jks` | Path to the release keystore file |
| `KHARCHA_KEYSTORE_PASSWORD` | *(None / Secure Env)* | Password for the keystore |
| `KHARCHA_KEY_ALIAS` | `kharcha_release_key` | Alias of the signing key entry |
| `KHARCHA_KEY_PASSWORD` | *(None / Secure Env)* | Password for the private key |

### Graceful Fallback
If `KHARCHA_KEYSTORE_PATH` (or default `kharcha-release.jks`) does not exist on disk, Gradle does not attach signing configuration to the `release` build type. This allows CI, unit tests, and local development builds (`assembleDebug`, `testDebugUnitTest`, `lintDebug`) to pass without requiring access to release secrets.

### Release Keystore Generation
To generate the permanent release keystore:
```bash
keytool -genkeypair \
  -v \
  -keystore kharcha-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias kharcha_release_key \
  -dname "CN=Kharcha Release, O=Kharcha, C=NP"
```

### Security Guard Enforcements
- Git ignores keystores: `*.jks`, `*.keystore`, and `*.p12` are listed in `.gitignore`.
- Automated pre-commit guard `scripts/guard-secrets.sh` fails any commit attempting to stage keystores, `.jks`, or private keys.

---

## 3. Storage Hierarchy & OS Preservation

Android OS preserves the entire app-private sandbox across package updates. Kharcha stores all user assets strictly within app-private directories:

| Component | Path | OS Upgrade Retention |
|-----------|------|----------------------|
| **Room Database** | `context.getDatabasePath("app_database.db")` | Preserved across updates |
| **Receipt Images** | `File(context.filesDir, "receipts/")` | Preserved across updates |
| **Document Attachments** | `File(context.filesDir, "documents/")` | Preserved across updates |
| **User Preferences** | `context.filesDir/datastore/user_preferences.preferences_pb` | Preserved across updates |
| **Temporary Staging** | `context.cacheDir` | Safe to clear; never used for permanent data |

---

## 4. Database Migration Invariant

Every database schema version bump requires:
1. An explicit `Migration(N, N+1)` implementation in `com.sijan.kharcha.data.db.migration`.
2. Registration in `DatabaseModule.kt` (`Room.databaseBuilder(...).addMigrations(...)`).
3. An automated schema migration test verifying data retention across the transition.
4. Exported Room schema JSON in `app/schemas/` committed to Git.
5. Enforcement by `scripts/guard-migrations.sh` blocking `fallbackToDestructiveMigration`.
