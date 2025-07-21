package com.example.myapplicationalt12

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class YOLOv8nDetector(context: Context) {
    private var detector: ObjectDetector
    private val inputSize = 640 // YOLOv8n input size (320x320)

    init {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(200) // Max books to detect
            .setScoreThreshold(0.5f) // Confidence threshold
            .setNumThreads(4) // Optimize for performance
            .build()

        // Load YOLOv8 TFLite model
        val modelFile = FileUtil.loadMappedFile(context, "yolov8n_float32.tflite")
        detector = ObjectDetector.createFromFileAndOptions(modelFile, options)
    }

    fun detectBooks(bitmap: Bitmap): List<Book> {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        // Preprocess image for YOLO
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // Normalize pixel values
            .build()

        val processedImage = imageProcessor.process(tensorImage)
        val detections = detector.detect(processedImage)

        return detections.map { detection ->
            Book(
                boundingBox = detection.boundingBox,
                confidence = detection.categories[0].score,
                label = detection.categories[0].label
            )
        }
    }

    data class Book(
        val boundingBox: RectF,
        val confidence: Float,
        val label: String? = null
    )

    fun close() {
        // Clean up resources if needed
    }
}