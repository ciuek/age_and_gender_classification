package com.example.age_and_gender_classification

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.age_and_gender_classification.ml.AgeModel
import com.example.age_and_gender_classification.ml.GenderModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt


class PreviewActivity : AppCompatActivity() {

    private val MODEL_INPUT_SIZE = 64
    private val PREVIEW_SIZE = 512
    private lateinit var resultFieldAge : TextView
    private lateinit var resultFieldGender : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val view = findViewById<ImageView>(R.id.imageView)
        resultFieldAge = findViewById(R.id.result_age)
        resultFieldGender = findViewById(R.id.result_gender)

        val fileName = intent.getStringExtra("name")

        val bmp = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + File.separator + fileName)
        val scaledbmp = Bitmap.createScaledBitmap(bmp, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, false)

        predict_age(scaledbmp)
        predict_gender(scaledbmp)

        view.setImageBitmap(Bitmap.createScaledBitmap(bmp, PREVIEW_SIZE, PREVIEW_SIZE, false))
    }

    @SuppressLint("SetTextI18n")
    private fun predict_age(bmp: Bitmap) {
        val model = AgeModel.newInstance(this)

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

        resultFieldAge.text = "Wiek: " + outputFeature0[0].roundToInt() + " ± 7 lat"

        model.close()

    }

    @SuppressLint("SetTextI18n")
    private fun predict_gender(bmp: Bitmap)
    {
        val model = GenderModel.newInstance(this)

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

        if(outputFeature0[0] < 0.5)
            resultFieldGender.text = "Płeć: Kobieta"
        else
            resultFieldGender.text = "Płeć: Mężczyzna"


        model.close()
    }

}