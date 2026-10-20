package com.sijan.kharcha.data.storage

import android.net.Uri
import java.io.InputStream

interface ReceiptStorage {
    suspend fun saveImage(inputStream: InputStream): Result<String>
    suspend fun saveImage(bytes: ByteArray): Result<String>
    fun getImageUri(filePath: String): Uri
    suspend fun deleteImage(filePath: String): Boolean

    suspend fun saveDocument(inputStream: InputStream, extension: String): Result<String>
    suspend fun saveDocument(bytes: ByteArray, extension: String): Result<String>
    fun getDocumentUri(filePath: String): Uri
}
