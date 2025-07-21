package com.example.myapplicationalt12

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detectorYOLO: YOLOv8nDetector
    private lateinit var procesadorOCR: ProcesadorOCR
    private lateinit var viewFinder: PreviewView
    private lateinit var textoResultados: TextView
    private lateinit var btnEscanear: FloatingActionButton
    private var isScanning = false
    private var imageAnalysis: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.viewFinder)
        textoResultados = findViewById(R.id.resultText)
        btnEscanear = findViewById(R.id.scanButton)
        detectorYOLO = YOLOv8nDetector(this)
        procesadorOCR = ProcesadorOCR("${filesDir.absolutePath}/tesseract/")

        btnEscanear.setOnClickListener {
            if (!isScanning) {
                startScanning()
            } else {
                stopScanning()
            }
        }

        //==========================PERMISOS DE CAMARA===========================================================
        if (allPermissionsGranted()) {
            setupCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        //==========================FIN PERMISOS DE CAMARA===========================================================
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch(exc: Exception) {
                Log.e("MainActivity", "Caso de uso fallido", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startScanning() {
        isScanning = true
        btnEscanear.setImageResource(android.R.drawable.ic_media_pause)
        textoResultados.text = "Escaneando libros..."
        imageAnalysis?.setAnalyzer(
            cameraExecutor,
            CameraAnalyzer(detectorYOLO, procesadorOCR) { libros ->
                runOnUiThread {
                    if (libros.isNotEmpty()) {
                        textoResultados.text = "Found ${libros.size} books:\n" +
                                libros.joinToString("\n") { it.label ?: "Libro" }
                    } else {
                        textoResultados.text = "No se detectaron libros"
                    }
                }
            }
        )
    }

    private fun stopScanning() {
        isScanning = false
        btnEscanear.setImageResource(android.R.drawable.ic_search_category_default)
        textoResultados.text = "Presione 'Escanear' para detectar libros"
        imageAnalysis?.clearAnalyzer()
    }

    private fun updateUI(books: List<YOLOv8nDetector.Book>) {
        // Update UI with detected books (e.g., show in RecyclerView)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        detectorYOLO.close()
        procesadorOCR.close()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}