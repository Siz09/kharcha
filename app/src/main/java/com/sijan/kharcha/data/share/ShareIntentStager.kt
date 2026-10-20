package com.sijan.kharcha.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

enum class SharedDocumentType {
    IMAGE,
    PDF,
    DOCX,
    DOC,
    UNSUPPORTED
}

data class StagedSharePayload(
    val stagedUri: Uri,
    val localFilePath: String,
    val mimeType: String,
    val documentType: SharedDocumentType,
    val displayName: String? = null
)

object ShareIntentStager {

    private const val STAGING_SUBDIR = "shared_staging"

    fun determineDocumentType(mimeType: String?, fileName: String? = null): SharedDocumentType {
        if (mimeType == null && fileName == null) return SharedDocumentType.UNSUPPORTED
        val lowerMime = mimeType?.lowercase().orEmpty()
        val lowerName = fileName?.lowercase().orEmpty()

        return when {
            lowerMime.startsWith("image/") ||
                lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") ||
                lowerName.endsWith(".webp") -> {
                SharedDocumentType.IMAGE
            }
            lowerMime == "application/pdf" || lowerName.endsWith(".pdf") -> {
                SharedDocumentType.PDF
            }
            lowerMime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                lowerName.endsWith(".docx") -> {
                SharedDocumentType.DOCX
            }
            lowerMime == "application/msword" || lowerName.endsWith(".doc") -> {
                SharedDocumentType.DOC
            }
            else -> SharedDocumentType.UNSUPPORTED
        }
    }

    fun stageFromIntent(context: Context, intent: Intent?): StagedSharePayload? {
        if (intent == null) return null
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null

        val sourceUri = (when (action) {
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: intent.data
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                list?.firstOrNull() ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }
            else -> null
        }) ?: return null

        val mimeType = intent.type ?: context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
        val displayName = sourceUri.lastPathSegment
        val documentType = determineDocumentType(mimeType, displayName)

        if (documentType == SharedDocumentType.UNSUPPORTED) {
            return null
        }

        return stageStream(context, sourceUri, mimeType, documentType, displayName)
    }

    fun cleanStagingDirectory(context: Context) {
        val stagingDir = File(context.cacheDir, STAGING_SUBDIR)
        if (stagingDir.exists() && stagingDir.isDirectory) {
            val now = System.currentTimeMillis()
            stagingDir.listFiles()?.forEach { file ->
                // Clean staged files older than 1 hour
                if (now - file.lastModified() > 3600_000L) {
                    file.delete()
                }
            }
        }
    }

    private fun stageStream(
        context: Context,
        sourceUri: Uri,
        mimeType: String,
        docType: SharedDocumentType,
        displayName: String?
    ): StagedSharePayload? {
        val stagingDir = File(context.cacheDir, STAGING_SUBDIR).apply {
            if (!exists()) mkdirs()
        }

        val extension = when (docType) {
            SharedDocumentType.IMAGE -> ".jpg"
            SharedDocumentType.PDF -> ".pdf"
            SharedDocumentType.DOCX -> ".docx"
            SharedDocumentType.DOC -> ".doc"
            SharedDocumentType.UNSUPPORTED -> ".bin"
        }

        val targetFile = File(stagingDir, "shared_${UUID.randomUUID()}$extension")

        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IOException("Unable to open stream for $sourceUri")

            inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val stagedUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            } catch (e: Exception) {
                Uri.fromFile(targetFile)
            }

            StagedSharePayload(
                stagedUri = stagedUri,
                localFilePath = targetFile.absolutePath,
                mimeType = mimeType,
                documentType = docType,
                displayName = displayName
            )
        } catch (e: Exception) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            null
        }
    }
}
