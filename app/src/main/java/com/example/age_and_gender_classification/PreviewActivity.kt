package com.example.age_and_gender_classification

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.os.Parcelable
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.LocalModel
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class PreviewActivity : AppCompatActivity() {

    private val MODEL_INPUT_SIZE = 64
    private val BATCH_SIZE = 128

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val view = findViewById<ImageView>(R.id.imageView)

        val byteArray = intent.getByteArrayExtra("image")
        val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

        view.setImageBitmap(bmp)
    }

//
//    @Throws(IOException::class)
//    private fun getModelByteBuffer(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
//        val fileDescriptor = assetManager.openFd(modelPath)
//        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
//        val fileChannel = inputStream.channel
//        val startOffset = fileDescriptor.startOffset
//        val declaredLength = fileDescriptor.declaredLength
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//    }
//
    fun recognize(bitmap: Bitmap): Float{
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, false)
        val pixelValues = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        scaledBitmap.getPixels(pixelValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)


        return
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun doInference(inputString: String): Float {
        val inputVal = FloatArray(1)
        inputVal[0] = inputString.toFloat()
        val output = Array(1) { FloatArray(1) }
        interpreter.run(inputVal, output)
        return output[0][0]
    }
}