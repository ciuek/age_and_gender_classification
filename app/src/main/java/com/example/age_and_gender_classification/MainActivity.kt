package com.example.age_and_gender_classification

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.age_and_gender_classification.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection.getClient
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity()  {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture:ImageCapture?=null
    private lateinit var cameraExecutor: ExecutorService

    val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val inputimage = InputImage.fromBitmap(bitmap!!, 0)
        analyzePhoto(inputimage)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getpermission)
            }
        }

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
            getContent.launch("image/*")
        }
    }

    private fun takePhoto(){
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("UnsafeOptInUsageError")
                override fun onCaptureSuccess(image: ImageProxy) {
                    val inputimage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
                    analyzePhoto(inputimage)
//                    analyze(image)
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

    private fun analyzePhoto(image: InputImage){
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
                when(faces.size)
                {
                    0 -> {
                        Toast.makeText(this,
                            "Nie wykryto twarzy!",
                            Toast.LENGTH_SHORT).show()
                    }

                    1 -> {
                        val bitmap_img = image.bitmapInternal!!//mediaImage.toBitmap()
                        val face = faces[0]
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
                            if(y + width > bitmap_img.height) bitmap_img.height - y else height
                        )

                        val intent = Intent(this, PreviewActivity::class.java)
                        val name = "${UUID.randomUUID()}.jpg"
                        val file = save_img(crop, name)

                        if(file != null)
                        {
                            intent.putExtra("name", name)
                            startActivity(intent)
                        }
                        else
                        {
                            Toast.makeText(this,
                                "Nie można utworzyć pliku!",
                                Toast.LENGTH_SHORT).show()
                        }

                    }
                    else ->
                        Toast.makeText(this,
                            "Wykryto więcej niż jedną twarz!",
                            Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(Constants.TAG,"onError: ${e.message}", e)
            }

    }

    private fun save_img(img: Bitmap, name: String): File? {
            var file: File? = null
            return try {
                file = File(Environment.getExternalStorageDirectory().toString() + File.separator + name)
                file.createNewFile()

                val bos = ByteArrayOutputStream()
                img.compress(Bitmap.CompressFormat.PNG, 100, bos) // YOU can also save it in JPEG
                val bitmapdata = bos.toByteArray()

                val fos = FileOutputStream(file)
                fos.write(bitmapdata)
                fos.flush()
                fos.close()
                file
            } catch (e: Exception) {
                e.printStackTrace()
                file
            }
    }
}