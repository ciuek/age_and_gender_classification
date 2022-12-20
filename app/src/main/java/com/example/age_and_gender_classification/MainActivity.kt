package com.example.age_and_gender_classification

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.age_and_gender_classification.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection.getClient
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
//import kotlin.coroutines.jvm.internal.CompletedContinuation.context


class MainActivity : AppCompatActivity(), ImageAnalysis.Analyzer  {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture:ImageCapture?=null
    private lateinit var  outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()


        if(allPermissionGranted()) {
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this, Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.take.setOnClickListener {
            takePhoto()
        }
        binding.choose.setOnClickListener {
            choosephoto()
        }
    }

    private fun choosephoto() {
        val image: InputImage
        try {
         //   image = InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun takePhoto(){
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    analyze(image)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG,"onError: ${exception.message}", exception)
                }
            }
        )
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { mPreview->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            }catch (e: Exception){
                Log.d(Constants.TAG,"startCamera Fail: ", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else{
                Toast.makeText(this,
                    "Permission not granted",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun analyze(imageproxy: ImageProxy) {
        val mediaImage = imageproxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageproxy.imageInfo.rotationDegrees)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build()
            val detector = getClient(options)

            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    if(faces.size == 0)
                    {
                        Toast.makeText(this,
                            "Nie wykryto twarzy!",
                            Toast.LENGTH_SHORT).show()
                    }
                    val bitmap_img = image.bitmapInternal!!//mediaImage.toBitmap()
                    for (face in faces) {
                        val bounds = face.boundingBox

                        val x = Math.max(bounds.left, 0)
                        val y = Math.max(bounds.top, 0)

                        val width = bounds.width()
                        val height = bounds.height()

                        bounds.set(
                            bounds.left,
                            bounds.top,
                            bounds.right,
                            bounds.bottom
                        )

                        val crop = Bitmap.createBitmap(
                            bitmap_img,
                            x,
                            y,
                            if(x + width > bitmap_img.width) bitmap_img.width - x else width,
                            if(y + width > bitmap_img.height) bitmap_img.height - x else height
                        )

                        val stream = ByteArrayOutputStream()
                        crop.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val byteArray = stream.toByteArray()

                        val intent = Intent(this, PreviewActivity::class.java)
                        intent.putExtra("image", byteArray)

                        startActivity(intent)

                    }
                }
                .addOnFailureListener { e ->
                    Log.e(Constants.TAG,"onError: ${e.message}", e)
                }

        }
    }
}