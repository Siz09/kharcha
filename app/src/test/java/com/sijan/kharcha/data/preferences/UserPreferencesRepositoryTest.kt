package com.sijan.kharcha.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = UserPreferencesRepositoryImpl(context)
    }

    @Test
    fun biometricLockEnabled_defaultsToFalse_andUpdatesCorrectly() = runTest {
        val initial = repository.preferences.first()
        assertFalse(initial.biometricLockEnabled)

        repository.setBiometricLockEnabled(true)
        val updated = repository.preferences.first()
        assertTrue(updated.biometricLockEnabled)

        repository.setBiometricLockEnabled(false)
        val reverted = repository.preferences.first()
        assertFalse(reverted.biometricLockEnabled)
    }
}
