package com.example.beasiswaku_kel2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import ai.onnxruntime.*

class MainActivity : AppCompatActivity() {

    private lateinit var ortEnv: OrtEnvironment
    private lateinit var ortSession: OrtSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi ONNX Runtime + load model
        loadONNXModel()

        val avgInput  = findViewById<EditText>(R.id.inputAvgGrade)
        val meduInput = findViewById<EditText>(R.id.inputMedu)
        val feduInput = findViewById<EditText>(R.id.inputFedu)
        val btnPredict = findViewById<Button>(R.id.btnPredict)
        val textResult = findViewById<TextView>(R.id.textResult)

        btnPredict.setOnClickListener {

            if (avgInput.text.isEmpty() || meduInput.text.isEmpty() || feduInput.text.isEmpty()) {
                textResult.text = "Harap isi semua kolom!"
                return@setOnClickListener
            }

            val avg  = avgInput.text.toString().toFloat()
            val medu = meduInput.text.toString().toFloat()
            val fedu = feduInput.text.toString().toFloat()

            val probYes = predictScholarship(avg, medu, fedu)
            val percent = String.format("%.2f", probYes * 100f)
            val label = if (probYes >= 0.5f) "LAYAK BEASISWA" else "TIDAK LAYAK BEASISWA"

            textResult.text = "Hasil: $label\nProbabilitas: $percent %"
        }
    }

    // ======================== LOAD MODEL ONNX ============================
    private fun loadONNXModel() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()

            // letakkan gradient_boosting_best.onnx di folder app/src/main/assets
            val modelData = assets.open("gradient_boosting_best.onnx").readBytes()

            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(1)
            }

            ortSession = ortEnv.createSession(modelData, options)
            Log.d("ONNX", "Model ONNX berhasil dimuat")
        } catch (e: Exception) {
            Log.e("ONNX", "Gagal memuat model: ${e.message}")
        }
    }

    // ======================== PREDIKSI ================================
    private fun predictScholarship(avg: Float, medu: Float, fedu: Float): Float {
        return try {
            val inputData = arrayOf(floatArrayOf(avg, medu, fedu))  // [1,3]
            val tensor = OnnxTensor.createTensor(ortEnv, inputData)

            val result = ortSession.run(mapOf("float_input" to tensor))

            // result[0] = label [0/1] (ignore if hanya mau probabilitas)
            // result[1] = probabilities [[p_No, p_Yes]]
            val probsValue = result[1].value

            val probYes: Float = when (probsValue) {
                is Array<*> -> {
                    val arr = probsValue as Array<FloatArray>
                    // Debug
                    Log.d("ONNX", "Probabilities[0] = ${arr[0].joinToString()}")
                    arr[0][1]     // indeks 1 = Yes (LAYAK)
                }
                is FloatArray -> {
                    Log.d("ONNX", "Probabilities = ${probsValue.joinToString()}")
                    probsValue[1]
                }
                else -> {
                    Log.e("ONNX", "Unknown probs type: ${probsValue::class.java}")
                    0f
                }
            }

            probYes
        } catch (e: Exception) {
            Log.e("ONNX", "Error prediksi: ${e.message}")
            0f
        }
    }
}
