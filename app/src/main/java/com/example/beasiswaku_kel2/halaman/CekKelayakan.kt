package com.example.beasiswaku_kel2

import ai.onnxruntime.*
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.beasiswaku_kel2.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class CekKelayakan : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var session: OrtSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ================= INISIALISASI ONNX =================
        ortEnv = OrtEnvironment.getEnvironment()

        val modelFile = File(
            getDir("models", MODE_PRIVATE),
            "gradient_boosting_best.onnx"
        )

        copyAssetToInternalStorage("gradient_boosting_best.onnx", modelFile)

        val sessionOptions = OrtSession.SessionOptions()
        session = ortEnv.createSession(modelFile.absolutePath, sessionOptions)

        // ================= TOMBOL PREDIKSI =================
        binding.btnPredict.setOnClickListener {
            try {
                val avgStr = binding.inputAvgGrade.text.toString().trim()
                val meduStr = binding.inputMedu.text.toString().trim()
                val feduStr = binding.inputFedu.text.toString().trim()

                if (avgStr.isEmpty() || meduStr.isEmpty() || feduStr.isEmpty()) {
                    Toast.makeText(this, "Semua input harus diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val avg = avgStr.toFloat()
                val medu = meduStr.toFloat()
                val fedu = feduStr.toFloat()

                // ================= INPUT TENSOR [1,3] =================
                val inputArray = floatArrayOf(avg, medu, fedu)
                val buffer = FloatBuffer.wrap(inputArray)

                val tensor = OnnxTensor.createTensor(
                    ortEnv,
                    buffer,
                    longArrayOf(1, 3)
                )

                // ================= RUN MODEL =================
                val outputs = session.run(
                    mapOf("float_input" to tensor)
                )

                // Output probabilities [[p_no, p_yes]]
                val probs = outputs[1].value as Array<FloatArray>
                val probYes = probs[0][1]

                val percent = String.format("%.2f", probYes * 100f)

                val label = if (probYes >= 0.5f)
                    "LAYAK BEASISWA"
                else
                    "TIDAK LAYAK BEASISWA"

                // ================= CLEAN UP =================
                outputs.close()
                tensor.close()

                // ================= PINDAH KE HASIL TEST =================
                val intent = Intent(this, HasilTest::class.java).apply {
                    putExtra("AVG_GRADE", avg.toString())
                    putExtra("MEDU", medu.toString())
                    putExtra("FEDU", fedu.toString())
                    putExtra("PROBABILITY", percent)
                    putExtra("LABEL", label)
                }

                startActivity(intent)

            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Input harus berupa angka!", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // ================= COPY MODEL DARI ASSETS =================
    private fun copyAssetToInternalStorage(assetName: String, destFile: File) {
        if (!destFile.exists()) {
            assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session.close()
        ortEnv.close()
    }
}
