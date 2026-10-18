package com.sijan.kharcha.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiptStorage {

    private val receiptsDir: File
        get() = File(context.filesDir, "receipts").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    override suspend fun saveImage(inputStream: InputStream): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "receipt_${UUID.randomUUID()}.jpg"
            val targetFile = File(receiptsDir, fileName)
            targetFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            targetFile.absolutePath
        }
    }

    override suspend fun saveImage(bytes: ByteArray): Result<String> {
        return saveImage(ByteArrayInputStream(bytes))
    }

    override fun getImageUri(filePath: String): Uri {
        val file = File(filePath)
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            // Fallback for Robolectric on Windows where drive-letter casing difference causes SimplePathStrategy.startsWith() to fail
            Uri.parse("content://${context.packageName}.fileprovider/receipts/${file.name}")
        }
    }

    override suspend fun deleteImage(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }
}
