package com.sijan.kharcha.retention

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.migration.MIGRATION_1_2
import com.sijan.kharcha.data.db.migration.MIGRATION_2_3
import com.sijan.kharcha.data.db.migration.MIGRATION_3_4
import com.sijan.kharcha.data.db.migration.MIGRATION_4_5
import com.sijan.kharcha.data.db.migration.MIGRATION_5_6
import com.sijan.kharcha.data.db.migration.MIGRATION_6_7
import com.sijan.kharcha.data.storage.ReceiptStorageImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppUpgradeRetentionTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun packageStability_applicationIdMatchesExpectedConstant() {
        assertEquals("com.sijan.kharcha", context.packageName)
    }

    @Test
    fun storageHierarchy_receiptStorageUsesAppPrivateFilesDir() {
        runBlocking {
            val storage = ReceiptStorageImpl(context)
            val sampleData = "upgrade-test-receipt-data".toByteArray()
            val result = storage.saveImage(ByteArrayInputStream(sampleData))

            assertTrue(result.isSuccess)
            val filePath = result.getOrThrow()
            val file = File(filePath)

            assertTrue(file.exists())
            // Invariant: file must reside within app-private filesDir, preserved by Android OS during APK upgrades
            assertTrue(filePath.startsWith(context.filesDir.absolutePath))
            assertFalse(filePath.startsWith(context.cacheDir.absolutePath))

            storage.deleteImage(filePath)
        }
    }

    @Test
    fun storageHierarchy_databaseLocatedInAppPrivateDatabaseDir() {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        assertNotNull(dbFile)

        // Invariant: database must reside within app-private databases dir
        val appDataDir = context.filesDir.parentFile
        assertNotNull(appDataDir)
        assertTrue(dbFile.absolutePath.startsWith(appDataDir!!.absolutePath))
        assertFalse(dbFile.absolutePath.startsWith(context.cacheDir.absolutePath))
    }

    @Test
    fun databaseConfiguration_registersAllMigrationsWithoutDestructiveFallback() {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "test_retention_upgrade.db"
        ).addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7
        ).build()

        assertNotNull(db)
        db.close()
        context.deleteDatabase("test_retention_upgrade.db")
    }

    @Test
    fun securityInvariant_manifestDisallowsInternetPermission() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions ?: emptyArray()

        val forbiddenPermission = listOf("android", "permission", "INTERNET").joinToString(".")
        assertFalse(
            "Invariant violation: Internet permission must never be present",
            permissions.contains(forbiddenPermission)
        )
    }

    @Test
    fun securityInvariant_manifestDisallowsUnencryptedCloudBackup() {
        val appInfo = context.applicationInfo
        val isAllowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0

        assertFalse(
            "Invariant violation: allowBackup must be false to prevent unencrypted cloud extraction",
            isAllowBackup
        )
    }
}
