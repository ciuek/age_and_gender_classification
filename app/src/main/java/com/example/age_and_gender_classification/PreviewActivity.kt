package com.example.age_and_gender_classification

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.age_and_gender_classification.ml.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder


class PreviewActivity : AppCompatActivity() {

    private val MODEL_INPUT_SIZE = 64
    private val BATCH_SIZE = 128
    private lateinit var resultFieldAge : TextView

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val view = findViewById<ImageView>(R.id.imageView)
        resultFieldAge = findViewById(R.id.result_age);

        val byteArray = intent.getByteArrayExtra("image")
        val bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size), MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, false)

        predict_age(bmp)

        view.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
    }

    private fun predict_age(bmp: Bitmap) {
        val model = AutoModel11ImdbNewestfin.newInstance(this)

//        val stream = ByteArrayOutputStream()
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        val array = stream.toByteArray()

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, 3), DataType.FLOAT32)
        val byteBuffer = ByteBuffer.allocateDirect(4 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        bmp.getPixels(intValues, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        var pixel = 0

        for (i in 0 until MODEL_INPUT_SIZE)
        {
            for(j in 0 until MODEL_INPUT_SIZE)
            {
                val pixelValue = intValues[pixel++]
                byteBuffer.putFloat((pixelValue shr 16 and 0xFF) / 1f)
                byteBuffer.putFloat((pixelValue shr 8 and 0xFF) / 1f)
                byteBuffer.putFloat((pixelValue and 0xFF) / 1f)
            }
        }

        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

        resultFieldAge.text = outputFeature0[0].toString()

        model.close()

    }

}