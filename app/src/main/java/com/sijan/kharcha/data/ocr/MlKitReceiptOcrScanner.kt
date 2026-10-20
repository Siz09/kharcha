package com.sijan.kharcha.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sijan.kharcha.domain.parser.ReceiptOcrScanner
import com.sijan.kharcha.domain.parser.model.BoundingBox
import com.sijan.kharcha.domain.parser.model.OcrBlock
import com.sijan.kharcha.domain.parser.model.OcrLine
import com.sijan.kharcha.domain.parser.model.OcrResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitReceiptOcrScanner @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiptOcrScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun scanImage(imageUri: Uri): Result<OcrResult> = withContext(Dispatchers.IO) {
        runCatching {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            processInputImage(inputImage)
        }
    }

    override suspend fun scanBitmap(bitmap: android.graphics.Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
        runCatching {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            processInputImage(inputImage)
        }
    }

    private suspend fun processInputImage(inputImage: InputImage): OcrResult = suspendCancellableCoroutine { continuation ->
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    val lines = block.lines.map { line ->
                        val box = line.boundingBox?.let { rect ->
                            BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
                        }
                        OcrLine(text = line.text, boundingBox = box)
                    }
                    val blockBox = block.boundingBox?.let { rect ->
                        BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
                    }
                    OcrBlock(text = block.text, lines = lines, boundingBox = blockBox)
                }
                val result = OcrResult(
                    fullText = visionText.text,
                    blocks = blocks
                )
                continuation.resume(result)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWith(Result.failure(exception))
            }
    }
}
