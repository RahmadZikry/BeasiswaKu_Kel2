package com.example.beasiswaku_kel2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView

class HasilTest : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_test)

        Log.d("DEBUG", "HasilTest onCreate")

        try {
            // Ambil data dari intent
            val avgGrade = intent.getStringExtra("AVG_GRADE") ?: "0"
            val medu = intent.getStringExtra("MEDU") ?: "0"
            val fedu = intent.getStringExtra("FEDU") ?: "0"
            val probability = intent.getStringExtra("PROBABILITY") ?: "0"
            val label = intent.getStringExtra("LABEL") ?: "TIDAK LAYAK"

            Log.d("DEBUG", "Received data: avg=$avgGrade, medu=$medu, fedu=$fedu, prob=$probability")

            // Tampilkan data input (gunakan ID yang benar dari XML)
            findViewById<TextView>(R.id.textAvgGradeValue).text = avgGrade
            findViewById<TextView>(R.id.textMeduValue).text = medu
            findViewById<TextView>(R.id.textFeduValue).text = fedu

            // Tampilkan hasil
            findViewById<TextView>(R.id.textResultTitle).text = label
            findViewById<TextView>(R.id.textResultDescription).text = "Probabilitas: $probability%"

            // Tombol Kembali
            findViewById<Button>(R.id.btnBack).setOnClickListener {
                finish()
            }

            // Tombol Bagikan (opsional, bisa di-comment dulu)
            val btnShare = findViewById<Button>(R.id.btnShare)
            btnShare?.setOnClickListener {
                shareResult(avgGrade, medu, fedu, probability, label)
            }

        } catch (e: Exception) {
            Log.e("DEBUG", "Error in HasilTest: ${e.message}", e)
            finish() // Kembali jika error
        }
    }

    private fun shareResult(
        avgGrade: String,
        medu: String,
        fedu: String,
        probability: String,
        label: String
    ) {
        try {
            val shareMessage = """
                Hasil Prediksi Beasiswa:
                
                Nilai Rata-rata: $avgGrade
                Pendidikan Ibu: $medu
                Pendidikan Ayah: $fedu
                
                Status: $label
                Probabilitas: $probability%
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }

            startActivity(Intent.createChooser(intent, "Bagikan Hasil"))
        } catch (e: Exception) {
            Log.e("DEBUG", "Error sharing: ${e.message}")
        }
    }
}