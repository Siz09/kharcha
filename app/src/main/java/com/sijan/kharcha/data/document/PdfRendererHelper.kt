package com.sijan.kharcha.data.document

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object PdfRendererHelper {

    fun getPageCount(file: File): Int {
        if (!file.exists() || file.length() == 0L) return 0
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (_: Throwable) {
            0
        }
    }

    fun renderPage(file: File, pageIndex: Int = 0, targetWidth: Int = 1080): Result<Bitmap> {
        return runCatching {
            if (!file.exists()) {
                throw IllegalArgumentException("PDF file does not exist: ${file.absolutePath}")
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                        throw IndexOutOfBoundsException("Page index $pageIndex out of bounds for count ${renderer.pageCount}")
                    }
                    val page = renderer.openPage(pageIndex)
                    try {
                        val aspectRatio = page.height.toFloat() / page.width.toFloat()
                        val width = targetWidth.coerceAtMost(page.width * 2).coerceAtLeast(100)
                        val height = (width * aspectRatio).toInt().coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    } finally {
                        page.close()
                    }
                }
            }
        }
    }
}
