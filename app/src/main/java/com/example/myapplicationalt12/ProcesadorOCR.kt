package com.example.myapplicationalt12

import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI

class ProcesadorOCR(private val dataPath: String) {
    private val tessApi = TessBaseAPI()

    init {
        tessApi.init(dataPath, "eng") // Load English OCR model
    }

    fun extractText(bitmap: Bitmap, region: Rect? = null): String {
        tessApi.setImage(bitmap)
        region?.let { tessApi.setRectangle(it) }
        return tessApi.utF8Text ?: ""
    }

    fun close() {
        tessApi.end()
    }
}