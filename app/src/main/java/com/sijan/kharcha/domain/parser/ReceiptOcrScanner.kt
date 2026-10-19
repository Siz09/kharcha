package com.sijan.kharcha.domain.parser

import android.net.Uri
import com.sijan.kharcha.domain.parser.model.OcrResult

interface ReceiptOcrScanner {
    suspend fun scanImage(imageUri: Uri): Result<OcrResult>
}
