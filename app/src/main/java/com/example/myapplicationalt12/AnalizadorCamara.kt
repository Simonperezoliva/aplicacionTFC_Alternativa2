package com.example.myapplicationalt12

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.core.motion.utils.Utils
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

class CameraAnalyzer(
    private val yoloDetector: YOLOv8nDetector,
    private val ocrProcessor: ProcesadorOCR,
    private val onBooksDetected: (List<YOLOv8nDetector.Book>) -> Unit) : ImageAnalysis.Analyzer {
    private var frameCount = 0
    private val frameSkip = 3 // Process every 3rd frame to reduce load

    override fun analyze(image: ImageProxy) {
        try {
            frameCount++
            if (frameCount % frameSkip != 0) {
                image.close()
                return
            }
            val bitmap = image.toBitmap()
            val books = yoloDetector.detectBooks(bitmap)

            // Only run OCR on the most confident detection to save processing
            books.maxByOrNull { it.confidence }?.let { book ->
                val cropped = cropBookSpine(bitmap, book.boundingBox)
                val text = ocrProcessor.extractText(cropped)
                Log.d("Detector de Libros", "Texto Detectado: $text")
            }

            onBooksDetected(books)
        } catch (e: Exception) {
            Log.e("Analizador de Camara", "Error analizando imagen", e)
        } finally {
            image.close()
        }
    }

    private fun cropBookSpine(bitmap: Bitmap, boundingBox: RectF): Bitmap {
        val x = boundingBox.left.toInt().coerceAtLeast(0)
        val y = boundingBox.top.toInt().coerceAtLeast(0)
        val width = boundingBox.width().toInt().coerceAtMost(bitmap.width - x)
        val height = boundingBox.height().toInt().coerceAtMost(bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val plane = planes[0].buffer
        val bytes = ByteArray(plane.remaining())
        plane.get(bytes)

        val mat = Mat(height, width, CvType.CV_8UC1)
        mat.put(0, 0, bytes)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}