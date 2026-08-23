package com.example.finalproj.security

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.IntruderLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IntruderCaptureActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private var attemptNumber: Int = 0
    private var frontImagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No layout → instant white screen
        attemptNumber = intent.getIntExtra("attempt_number", 0)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (allPermissionsGranted()) {
            startCamera()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            finish()
        }
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageCapture
                )

                // Small delay to allow camera pipeline to initialize
                Handler(Looper.getMainLooper()).postDelayed({
                    captureImage()
                }, 250)

            } catch (e: Exception) {
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {

        val photoFile = createImageFile()

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    frontImagePath = photoFile.absolutePath
                    saveLogAndFinish()
                }

                override fun onError(exception: ImageCaptureException) {
                    finish()
                }
            }
        )
    }

    private fun createImageFile(): File {

        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val storageDir = File(filesDir, "intruder")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, "IMG_$timeStamp.jpg")
    }

    private fun saveLogAndFinish() {

        val log = IntruderLog(
            attemptNumber = attemptNumber,
            timestamp = System.currentTimeMillis(),
            frontImagePath = frontImagePath,
            backImagePath = "",
            deviceModel = Build.MODEL
        )

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(applicationContext)
                .intruderLogDao()
                .insertLog(log)
        }

        finish()
    }
}