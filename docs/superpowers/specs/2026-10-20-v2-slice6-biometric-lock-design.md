# Kharcha v2 Slice 6: Biometric Security Lock Design Spec

**Date**: 2026-10-20  
**Status**: APPROVED  
**Author**: Sijan & Antigravity  
**Target Milestone**: v2.0 (Slice 6)

---

## 1. Overview & Goals

Financial data in Kharcha (account balances, scanned receipts, udhar records) is sensitive personal data. While Kharcha is 100% offline with zero network leakage, on-device physical privacy is essential.

### Goals
1. **Device-Level Protection**: Lock the app behind Android's standard `BiometricPrompt`, supporting Fingerprint, Face Unlock, and device PIN/Pattern fallback.
2. **Background Auto-Lock**: Automatically lock the app when it moves to the background (app switcher, home screen, or screen turned off).
3. **Recents Obscuration**: Enable `FLAG_SECURE` on the Android window when App Lock is active, preventing sensitive balances from appearing in the Android recent-apps task switcher.
4. **Zero Lockout Guarantee**: Support `Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL` so that users are never permanently locked out if biometric sensors fail or fingers are wet.
5. **Impeccable & Animotion Polish**: Refined lock screen overlay adhering to Impeccable UI principles (tinted surfaces, accessible touch targets, no generic cards) and Animotion micro-interactions (FN25 secure lock entrance, haptic feedback).

### Non-Goals
- Custom in-app numeric PIN storage or password creation (platform credentials handle this securely).
- Remote or cloud lock recovery (violates offline invariant).
- Per-account or per-entry locking (whole app locking is safer and simpler).

---

## 2. Architecture & Data Flow

### 2.1 Component Architecture

```
[ ProcessLifecycleOwner ]
         │ (onStop -> auto-lock)
         ▼
[ MainActivity (FragmentActivity) ] ──(FLAG_SECURE)──> [ Window ]
         │
         ├─── observes ───> [ UserPreferences.biometricLockEnabled ]
         │
         ├─── manages ────> [ isAppUnlocked: StateFlow<Boolean> ]
         │
         ├─── uses ───────> [ BiometricAuthManager ]
         │                          │
         │                          └──> [ BiometricPrompt / BiometricManager ]
         │
         └─── renders ────> [ LockScreenOverlay ] (when locked)
                                    │
                                    └──> [ KharchaNavHost ] (underneath, blocked)
```

### 2.2 Domain & Preferences Layer
- **`UserPreferences`**:
  - Add `val biometricLockEnabled: Boolean = false` to `UserPreferences` in `domain/model/AppTheme.kt`.
- **`UserPreferencesRepository`**:
  - Add `suspend fun setBiometricLockEnabled(enabled: Boolean)` to interface and `UserPreferencesRepositoryImpl`.
  - Stored in DataStore Preferences using `booleanPreferencesKey("biometric_lock_enabled")`.

### 2.3 Security Layer (`data/security/BiometricAuthManager.kt`)
- Encapsulates interaction with `androidx.biometric.BiometricManager` and `androidx.biometric.BiometricPrompt`.
- Methods:
  - `canAuthenticate(): BiometricCapability` returns one of:
    - `AVAILABLE`: Hardware present and enrolled.
    - `NONE_ENROLLED`: Hardware present but no biometric or device PIN enrolled.
    - `NO_HARDWARE`: Device lacks secure hardware.
    - `UNSUPPORTED`: Biometric features unavailable.
  - `authenticate(activity: FragmentActivity, title: String, subtitle: String, onResult: (BiometricAuthResult) -> Unit)`:
    - Sets authenticators to `BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL`.
    - Dispatches result callbacks: `Success`, `Failure(errorMessage)`, `Cancelled`.

### 2.4 Presentation Layer & Lifecycle

#### MainActivity
- Migrate class hierarchy from `ComponentActivity` to `androidx.fragment.app.FragmentActivity`.
- Lifecycle Observer: Register `DefaultLifecycleObserver` on `ProcessLifecycleOwner.get().lifecycle`.
  - When app transitions to `onStop`: set `isAppUnlocked.value = false`.
  - When app returns to `onStart` / `onResume`: if `biometricLockEnabled == true && !isAppUnlocked.value`, automatically launch biometric prompt.
- Window Flag:
  - If `prefs.biometricLockEnabled == true`, set `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)`.
  - If `prefs.biometricLockEnabled == false`, clear `FLAG_SECURE`.

#### LockScreenOverlay (`ui/security/LockScreenOverlay.kt`)
- Fullscreen surface with tinted background (`colorScheme.surface`).
- Displays:
  - Kharcha brand shield/lock icon with Animotion `FN25` secure-lock translation animation (`translateY(-3dp) -> 1dp -> 0dp`, 300ms ease-out).
  - App title "Kharcha" (`titleLarge`, tinted brand green).
  - Subtitle "App is locked" (`bodyMedium`, `colorScheme.onSurfaceVariant`).
  - Primary button "Unlock Kharcha" (`Button`, minimum 48dp height, icon + text) triggering the prompt.
  - Tactile haptic feedback (`HapticFeedbackType.TextHandleMove`) on button press and authentication events.

#### SettingsScreen Integration
- Under "Security" group:
  - Row with Lock icon, title "App Lock", subtitle "Unlock with fingerprint, face, or device PIN".
  - `Switch` toggle.
  - On toggle ON: verify `canAuthenticate()`. If available, show biometric prompt. Only on success, commit `setBiometricLockEnabled(true)`.
  - On toggle OFF: show biometric prompt. Only on success, commit `setBiometricLockEnabled(false)`.

---

## 3. Impeccable Design & Animotion Compliance

- **Surface & Tint**: Never pure black or pure gray. Light theme uses warm cream/stone surface; dark theme uses rich tinted slate (`0xFF131316`).
- **Typography & Hierarchy**: Clear contrast hierarchy. Title `titleLarge`, subtitle `bodyMedium`, button with `FilledTonalButton` or `Button` styled with rounded M3 corners.
- **Motion (Animotion FN25)**: Entrance animation on lock badge uses crisp spring/ease-out translation (`-3dp` to `+1dp` to `0dp`) rather than elastic or bounce curves.
- **Touch Targets**: Minimum 48dp touch targets on all interactive elements. Full accessibility semantics (`Role.Button`, content descriptions).

---

## 4. String Resources (`res/values/strings.xml`)

All user-facing strings placed in `strings.xml`:
- `app_lock_title`: "App Lock"
- `app_lock_description`: "Require fingerprint, face, or device PIN to open Kharcha"
- `app_lock_prompt_title`: "Unlock Kharcha"
- `app_lock_prompt_subtitle`: "Confirm your identity to access your ledger"
- `app_lock_unlock_button`: "Unlock Kharcha"
- `app_lock_locked_status`: "Kharcha is locked"
- `app_lock_not_enrolled_error`: "No screen lock or biometric enrolled on this device"
- `app_lock_no_hardware_error`: "Biometric security is not available on this device"
- `app_lock_auth_failed`: "Authentication failed. Tap to try again."

---

## 5. Testing & Verification Plan

1. **Unit Tests**:
   - `UserPreferencesRepositoryTest`: verify `biometricLockEnabled` persistence and default value (`false`).
   - `SettingsViewModelTest`: verify `setBiometricLockEnabled` updates preferences and handles capability checks.
2. **Build & Quality Gates**:
   - `rtk gradlew testDebugUnitTest` (all unit tests pass).
   - `rtk gradlew lintDebug` (zero lint errors).
   - `bash scripts/check.sh` (all 5 repository guards pass: manifest, money, migrations, strings, secrets).
   - `rtk gradlew assembleDebug` (APK compiles cleanly).
