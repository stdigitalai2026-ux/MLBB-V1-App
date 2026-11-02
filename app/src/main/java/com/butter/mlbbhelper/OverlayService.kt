package com.butter.mlbbhelper

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.R
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

open class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var tvWarning: TextView
    private lateinit var params: WindowManager.LayoutParams
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var tessBaseApi: TessBaseAPI
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buatNotifikasi())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        buatOverlayUI()
        siapkanOCR()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra("resultCode")) {
            val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("dataIntent", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Intent>("dataIntent")
            }
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            if (dataIntent != null) {
                mediaProjection = mpm.getMediaProjection(resultCode, dataIntent)
                mulaiMembacaLayar()
            }
        }
        return START_STICKY
    }

    private fun buatOverlayUI() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        tvWarning = overlayView.findViewById(R.id.tvWarning)
        val tvClose = overlayView.findViewById<TextView>(R.id.tvClose)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100

        windowManager.addView(overlayView, params)
        tvWarning.text = "Menyiapkan AI..."
        tvClose.setOnClickListener { stopSelf() }
    }

    private fun mulaiMembacaLayar() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        handler.postDelayed(object : Runnable {
            override fun run() {
                bacaLayar()
                handler.postDelayed(this, 2500)
            }
        }, 2500)
    }

    private fun bacaLayar() {
        val image = imageReader?.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val cropBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height / 2)
        tessBaseApi.setImage(cropBitmap)
        val recognizedText = tessBaseApi.utF8Text ?: ""
        
        if (recognizedText.isNotEmpty()) {
            prosesAnalisis(recognizedText)
        }
        cropBitmap.recycle()
        bitmap.recycle()
    }

    private fun prosesAnalisis(teksLayar: String) {
        val teks = teksLayar.lowercase()
        val heroKita = HeroData.cariHero("miya") ?: return
        val listLawan = mutableListOf<HeroData.Hero>()
        
        if (teks.contains("saber")) HeroData.cariHero("saber")?.let { listLawan.add(it) }
        if (teks.contains("gusion")) HeroData.cariHero("gusion")?.let { listLawan.add(it) }
        if (teks.contains("franco")) HeroData.cariHero("franco")?.let { listLawan.add(it) }
        if (teks.contains("esmeralda")) HeroData.cariHero("esmeralda")?.let { listLawan.add(it) }
        
        if (listLawan.isNotEmpty()) {
            val hasilAnalisa = HeroData.analisaMatchup(heroKita, listLawan)
            runOnUiThread {
                tvWarning.text = hasilAnalisa
                tvWarning.setTextColor(android.graphics.Color.WHITE)
                tvWarning.visibility = View.VISIBLE
            }
        } else {
            runOnUiThread {
                tvWarning.text = "Membaca draft..."
                tvWarning.setTextColor(android.graphics.Color.GRAY)
            }
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post { action() }
    }

    private fun siapkanOCR() {
        tessBaseApi = TessBaseAPI()
        val datapath = "${filesDir}/tesseract/"
        val dir = File(datapath + "tessdata/")
        if (!dir.exists()) dir.mkdirs()
        
        try {
            val configFile = File(dir, "eng.traineddata")
            if (!configFile.exists()) {
                val asset = assets.open("eng.traineddata")
                val out = FileOutputStream(configFile)
                asset.copyTo(out)
                out.close()
                asset.close()
            }
            tessBaseApi.init(datapath, "eng")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buatNotifikasi(): Notification {
        val channelId = "mlbb_helper_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "MLBB Helper", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MLBB Auto-Helper")
            .setContentText("Membaca layar game...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::overlayView.isInitialized) windowManager.removeView(overlayView)
        virtualDisplay?.release()
        mediaProjection?.stop()
        if (this::tessBaseApi.isInitialized) {
            tessBaseApi.end()
        }
    }
}
