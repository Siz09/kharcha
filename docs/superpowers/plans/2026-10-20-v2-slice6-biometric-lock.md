# Kharcha v2 Slice 6: Biometric Security Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement on-device Biometric Security Lock in Kharcha using `androidx.biometric:biometric` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` fallback, auto-lock on app backgrounding, `FLAG_SECURE` window obscuration, and an Impeccable + Animotion lock screen overlay.

**Architecture:** A DataStore preference `biometricLockEnabled` stores security state. `BiometricAuthManager` wraps Android's `BiometricPrompt` and capability detection. `MainActivity` migrates to `FragmentActivity`, observes app backgrounding via `DefaultLifecycleObserver` on `ProcessLifecycleOwner`, toggles `FLAG_SECURE`, and renders `LockScreenOverlay` until unlocked.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose Material 3, Androidx Biometric 1.2.0-alpha05, Lifecycle Runtime, Hilt, DataStore Preferences.

**Spec:** [docs/superpowers/specs/2026-10-20-v2-slice6-biometric-lock-design.md](file:///D:/vscode/kharcha/docs/superpowers/specs/2026-10-20-v2-slice6-biometric-lock-design.md)

## Global Constraints

- 100% offline: `android.permission.INTERNET` is strictly forbidden.
- Zero floating point for money: all balances remain `Long` in paisa.
- All user-facing strings must reside in `res/values/strings.xml` (no hard-coded strings in UI).
- Follow Impeccable design: tinted background surfaces (`0xFF131316` in dark mode), minimum 48dp touch targets, no generic cards.
- Animotion micro-interaction: `FN25` secure-lock translation (`translateY(-3dp) -> 1dp -> 0dp`, 300ms ease-out) on lock badge entrance.
- Always prefix CLI commands with `rtk`.

---

### Task 1: Add Biometric Dependency

**Files:**
- Modify: `gradle/libs.versions.toml:20-25, 45-50`
- Modify: `app/build.gradle.kts:80-95`

**Interfaces:**
- Produces: `androidx.biometric:biometric:1.2.0-alpha05` and `androidx-lifecycle-process` available in classpath.

- [ ] **Step 1: Add biometric & lifecycle-process to version catalog**

Edit `gradle/libs.versions.toml`:
Add to `[versions]`:
```toml
biometric = "1.2.0-alpha05"
```
Add to `[libraries]`:
```toml
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
androidx-lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "lifecycle" }
```

- [ ] **Step 2: Add dependencies to app/build.gradle.kts**

Add to `dependencies` block in `app/build.gradle.kts`:
```kotlin
implementation(libs.androidx.biometric)
implementation(libs.androidx.lifecycle.process)
```

- [ ] **Step 3: Verify build sync**

Run: `rtk gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
rtk git add gradle/libs.versions.toml app/build.gradle.kts
rtk git commit -m "chore: add androidx biometric and lifecycle process dependencies"
```

---

### Task 2: DataStore Preferences & Model Updates

**Files:**
- Modify: `app/src/main/java/com/sijan/kharcha/domain/model/AppTheme.kt:9-16`
- Modify: `app/src/main/java/com/sijan/kharcha/data/preferences/UserPreferencesRepository.kt:7-15`
- Modify: `app/src/main/java/com/sijan/kharcha/data/preferences/UserPreferencesRepositoryImpl.kt:20-80`
- Test: `app/src/test/java/com/sijan/kharcha/data/preferences/UserPreferencesRepositoryTest.kt`

**Interfaces:**
- Consumes: `UserPreferences`, `UserPreferencesRepository`
- Produces: `UserPreferences.biometricLockEnabled: Boolean`, `suspend fun setBiometricLockEnabled(enabled: Boolean)`

- [ ] **Step 1: Write failing test in UserPreferencesRepositoryTest**

In `app/src/test/java/com/sijan/kharcha/data/preferences/UserPreferencesRepositoryTest.kt`, add test case:
```kotlin
@Test
fun `biometricLockEnabled defaults to false and updates correctly`() = runTest {
    // Assert initial is false
    val initial = repository.preferences.first()
    assertFalse(initial.biometricLockEnabled)

    // Update to true
    repository.setBiometricLockEnabled(true)
    val updated = repository.preferences.first()
    assertTrue(updated.biometricLockEnabled)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.preferences.UserPreferencesRepositoryTest"`  
Expected: Compilation failure or test failure (symbol unresolved)

- [ ] **Step 3: Update model, interface, and implementation**

In `AppTheme.kt`:
```kotlin
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val lastBackupTimestamp: Long? = null,
    val rolloverEnabled: Boolean = false,
    val smsDetectionEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false
)
```

In `UserPreferencesRepository.kt`:
```kotlin
suspend fun setBiometricLockEnabled(enabled: Boolean)
```

In `UserPreferencesRepositoryImpl.kt`:
```kotlin
private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
```
Map `preferences` flow with `preferences[KEY_BIOMETRIC_LOCK] ?: false` and implement `setBiometricLockEnabled`:
```kotlin
override suspend fun setBiometricLockEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
        prefs[KEY_BIOMETRIC_LOCK] = enabled
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.preferences.UserPreferencesRepositoryTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/domain/model/AppTheme.kt app/src/main/java/com/sijan/kharcha/data/preferences/ app/src/test/java/com/sijan/kharcha/data/preferences/
rtk git commit -m "feat: add biometricLockEnabled to UserPreferences repository"
```

---

### Task 3: String Resources for App Lock

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: XML string keys for lock screen, settings, and biometric prompts.

- [ ] **Step 1: Add App Lock strings to strings.xml**

Add to `app/src/main/res/values/strings.xml`:
```xml
    <!-- App Lock & Biometrics -->
    <string name="app_lock_title">App Lock</string>
    <string name="app_lock_description">Require fingerprint, face, or device PIN to open Kharcha</string>
    <string name="app_lock_prompt_title">Unlock Kharcha</string>
    <string name="app_lock_prompt_subtitle">Confirm your identity to access your ledger</string>
    <string name="app_lock_unlock_button">Unlock Kharcha</string>
    <string name="app_lock_locked_status">Kharcha is locked</string>
    <string name="app_lock_not_enrolled_error">No screen lock or biometric enrolled on this device</string>
    <string name="app_lock_no_hardware_error">Biometric security is not available on this device</string>
    <string name="app_lock_auth_failed">Authentication failed. Tap to try again.</string>
```

- [ ] **Step 2: Run guard-strings to verify**

Run: `bash scripts/guard-strings.sh`  
Expected: PASS (0 hard-coded or malformed strings)

- [ ] **Step 3: Commit**

```bash
rtk git add app/src/main/res/values/strings.xml
rtk git commit -m "chore: add string resources for biometric app lock"
```

---

### Task 4: Biometric Auth Manager

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/security/BiometricAuthManager.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/security/BiometricAuthManagerTest.kt`

**Interfaces:**
- Produces: `BiometricCapability`, `BiometricAuthResult`, `BiometricAuthManager`

- [ ] **Step 1: Write unit test for BiometricCapability mapping**

Create `app/src/test/java/com/sijan/kharcha/data/security/BiometricAuthManagerTest.kt`:
```kotlin
package com.sijan.kharcha.data.security

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricAuthManagerTest {

    @Test
    fun `mapStatus maps biometric manager codes correctly`() {
        assertEquals(BiometricCapability.AVAILABLE, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_SUCCESS))
        assertEquals(BiometricCapability.NONE_ENROLLED, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED))
        assertEquals(BiometricCapability.NO_HARDWARE, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE))
        assertEquals(BiometricCapability.UNSUPPORTED, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.security.BiometricAuthManagerTest"`  
Expected: FAIL (unresolved class)

- [ ] **Step 3: Implement BiometricAuthManager**

Create `app/src/main/java/com/sijan/kharcha/data/security/BiometricAuthManager.kt`:
```kotlin
package com.sijan.kharcha.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricCapability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNSUPPORTED
}

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data class Failure(val errorMsg: String) : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
}

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val allowedAuthenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(): BiometricCapability {
        val manager = BiometricManager.from(context)
        return mapStatus(manager.canAuthenticate(allowedAuthenticators))
    }

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(BiometricAuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onResult(BiometricAuthResult.Cancelled)
                    } else {
                        onResult(BiometricAuthResult.Failure(errString.toString()))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(BiometricAuthResult.Failure("Authentication failed"))
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    companion object {
        fun mapStatus(status: Int): BiometricCapability = when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            else -> BiometricCapability.UNSUPPORTED
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.security.BiometricAuthManagerTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/data/security/ app/src/test/java/com/sijan/kharcha/data/security/
rtk git commit -m "feat: implement BiometricAuthManager with strong biometric and device credential fallback"
```

---

### Task 5: Lock Screen Overlay UI (Impeccable & Animotion)

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/ui/security/LockScreenOverlay.kt`

**Interfaces:**
- Produces: `@Composable fun LockScreenOverlay(onUnlockClick: () -> Unit, errorMessage: String? = null)`

- [ ] **Step 1: Implement LockScreenOverlay with Animotion FN25 micro-interaction**

Create `app/src/main/java/com/sijan/kharcha/ui/security/LockScreenOverlay.kt`:
```kotlin
package com.sijan.kharcha.ui.security

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R

/**
 * LockScreenOverlay blocks access to Kharcha financial records until authenticated.
 * Incorporates Animotion FN25 lock entrance translation and Impeccable accessible styling.
 */
@Composable
fun LockScreenOverlay(
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    val haptic = LocalHapticFeedback.current
    var animatedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animatedIn = true
    }

    // Animotion FN25 entrance curve: translateY(-3dp -> 0dp)
    val translationY by animateFloatAsState(
        targetValue = if (animatedIn) 0f else -12f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "lock_entrance_fn25"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .graphicsLayer { this.translationY = translationY },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.app_lock_prompt_title),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_lock_locked_status),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUnlockClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(0.dp))
                Text(
                    text = stringResource(R.string.app_lock_unlock_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Run lint to verify layout**

Run: `rtk gradlew lintDebug`  
Expected: PASS with 0 errors

- [ ] **Step 3: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/ui/security/LockScreenOverlay.kt
rtk git commit -m "feat: create LockScreenOverlay with Animotion FN25 micro-interaction"
```

---

### Task 6: Settings Screen & ViewModel Integration

**Files:**
- Modify: `app/src/main/java/com/sijan/kharcha/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/sijan/kharcha/ui/settings/SettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `UserPreferences.biometricLockEnabled`, `UserPreferencesRepository.setBiometricLockEnabled`
- Produces: App Lock toggle in Settings UI with biometric verification on enable/disable.

- [ ] **Step 1: Write test in SettingsViewModelTest**

Add test to `app/src/test/java/com/sijan/kharcha/ui/settings/SettingsViewModelTest.kt`:
```kotlin
@Test
fun `onToggleBiometricLock calls repository to update state`() = runTest {
    viewModel.onToggleBiometricLock(true)
    assertTrue(fakeRepository.preferences.first().biometricLockEnabled)

    viewModel.onToggleBiometricLock(false)
    assertFalse(fakeRepository.preferences.first().biometricLockEnabled)
}
```

- [ ] **Step 2: Update SettingsViewModel**

In `SettingsViewModel.kt`, add:
```kotlin
fun onToggleBiometricLock(enabled: Boolean) {
    viewModelScope.launch {
        userPreferencesRepository.setBiometricLockEnabled(enabled)
    }
}
```

- [ ] **Step 3: Update SettingsScreen with Security group**

In `SettingsScreen.kt`, add "Security" section under Appearance or Backup:
```kotlin
// Security Group
Text(
    text = stringResource(R.string.security_title), // or App Lock
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
)

SwitchPreferenceItem(
    title = stringResource(R.string.app_lock_title),
    description = stringResource(R.string.app_lock_description),
    checked = uiState.biometricLockEnabled,
    onCheckedChange = { checked ->
        onToggleBiometricLock(checked)
    }
)
```

- [ ] **Step 4: Run unit tests**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.ui.settings.SettingsViewModelTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/ui/settings/ app/src/test/java/com/sijan/kharcha/ui/settings/
rtk git commit -m "feat: add App Lock toggle to Settings screen and view model"
```

---

### Task 7: MainActivity & App Lifecycle Auto-Lock Integration

**Files:**
- Modify: `app/src/main/java/com/sijan/kharcha/MainActivity.kt`

**Interfaces:**
- Consumes: `FragmentActivity`, `ProcessLifecycleOwner`, `BiometricAuthManager`, `LockScreenOverlay`
- Produces: Auto-locking lifecycle observer, `FLAG_SECURE` window toggle, biometric prompt on start.

- [ ] **Step 1: Update MainActivity to extend FragmentActivity and wire lifecycle lock**

In `MainActivity.kt`:
- Change inheritance from `ComponentActivity` to `androidx.fragment.app.FragmentActivity`.
- Inject `BiometricAuthManager`.
- Add state: `private var isAppUnlocked by mutableStateOf(false)` (or `MutableStateFlow`).
- Register `DefaultLifecycleObserver` on `ProcessLifecycleOwner.get().lifecycle`:
  ```kotlin
  ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) {
          if (biometricLockEnabled) {
              isAppUnlocked = false
          }
      }
      override fun onStart(owner: LifecycleOwner) {
          if (biometricLockEnabled && !isAppUnlocked) {
              requestUnlock()
          }
      }
  })
  ```
- Toggle `FLAG_SECURE` on window:
  ```kotlin
  if (prefs.biometricLockEnabled) {
      window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
  }
  ```
- Render `LockScreenOverlay` if `prefs.biometricLockEnabled && !isAppUnlocked`, otherwise render `KharchaApp`.

- [ ] **Step 2: Run gradlew assembleDebug to verify compilation**

Run: `rtk gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/MainActivity.kt
rtk git commit -m "feat: wire FragmentActivity, ProcessLifecycleOwner auto-lock, and FLAG_SECURE"
```

---

### Task 8: Full Verification Gates Run

**Files:** None (verification step)

- [ ] **Step 1: Run all unit tests**

Run: `rtk gradlew testDebugUnitTest`  
Expected: All tests pass (>= 131 tests)

- [ ] **Step 2: Run Android Lint**

Run: `rtk gradlew lintDebug`  
Expected: 0 errors

- [ ] **Step 3: Run repository verification guards**

Run: `bash scripts/check.sh`  
Expected: All 5 guards PASS:
- `guard-manifest.sh` (No INTERNET permission)
- `guard-money.sh` (No Double/Float for money)
- `guard-migrations.sh` (Room schema preserved)
- `guard-strings.sh` (Zero hardcoded strings)
- `guard-secrets.sh` (No secrets)

- [ ] **Step 4: Build debug APK**

Run: `rtk gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL
