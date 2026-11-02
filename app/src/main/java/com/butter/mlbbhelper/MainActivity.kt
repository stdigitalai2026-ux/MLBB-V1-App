package com.butter.mlbbhelper

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.R

open class MainActivity : AppCompatActivity() {
    
    private val REQUEST_CODE_SCREEN_CAPTURE = 100
    private val REQUEST_CODE_OVERLAY = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStartML = findViewById<Button>(R.id.btnStartML)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener { cekIzinDanMulai() }

        btnStartML.setOnClickListener {
            Toast.makeText(this, "Membuka Mobile Legends...", Toast.LENGTH_SHORT).show()
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.mobile.legends")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    cekIzinDanMulai()
                } else {
                    Toast.makeText(this, "Mobile Legends tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Overlay Dimatikan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cekIzinDanMulai() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan aplikasi tampil di atas game dulu!", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        } else {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            @Suppress("DEPRECATION")
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                cekIzinDanMulai()
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, OverlayService::class.java)
                serviceIntent.putExtra("resultCode", resultCode)
                serviceIntent.putExtra("dataIntent", data)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                try {
                    val mlIntent = packageManager.getLaunchIntentForPackage("com.mobile.legends")
                    mlIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(mlIntent)
                } catch (e: Exception) {}
                finish()
            } else {
                Toast.makeText(this, "Izin baca layar ditolak.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
