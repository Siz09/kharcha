package com.sijan.kharcha.data.security

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricAuthManagerTest {

    @Test
    fun mapStatus_mapsBiometricManagerCodesCorrectly() {
        assertEquals(BiometricCapability.AVAILABLE, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_SUCCESS))
        assertEquals(BiometricCapability.NONE_ENROLLED, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED))
        assertEquals(BiometricCapability.NO_HARDWARE, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE))
        assertEquals(BiometricCapability.UNSUPPORTED, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE))
        assertEquals(BiometricCapability.UNSUPPORTED, BiometricAuthManager.mapStatus(BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED))
    }
}
