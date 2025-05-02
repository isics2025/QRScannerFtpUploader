package com.example.qrftpapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var btnScan: Button
    private lateinit var tvStatus: TextView
    private lateinit var ivQrGen: ImageView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startScan() else tvStatus.text = "Разрешение на камеру отклонено"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScan = findViewById(R.id.btnScan)
        tvStatus = findViewById(R.id.tvStatus)
        ivQrGen = findViewById(R.id.ivQrGen)

        btnScan.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else startScan()
        }
    }

    private fun startScan() {
        IntentIntegrator(this).setOrientationLocked(false).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { url ->
                tvStatus.text = "Сканировано: $url"
                downloadAndUpload(url)
            } ?: run { tvStatus.text = "Сканирование отменено" }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadAndUpload(fileUrl: String) {
        tvStatus.text = "Загрузка..."
        thread {
            try {
                val conn = URL(fileUrl).openConnection()
                conn.connect()
                val fileName = fileUrl.substringAfterLast('/')
                val file = File(cacheDir, fileName)
                conn.getInputStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                tvStatus.post { tvStatus.text = "Загружено, начинаю FTP..." }
                val ftp = FTPClient().apply {
                    connect("ftp.getshaon.com")
                    login("isics@karantina.co.il", "K7551tn777")
                    enterLocalPassiveMode()
                    setFileType(FTP.BINARY_FILE_TYPE)
                }
                FileInputStream(file).use { fis -> ftp.storeFile(fileName, fis) }
                ftp.logout(); ftp.disconnect()
                val ftpUrl = "ftp://isics@karantina.co.il:K7551tn777@ftp.getshaon.com/$fileName"
                runOnUiThread { generateQrCode(ftpUrl) }
            } catch (e: Exception) {
                tvStatus.post { tvStatus.text = "Ошибка: \${e.message}" }
            }
        }
    }

    private fun generateQrCode(text: String) {
        val bm = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) for (y in 0 until 512)
            bmp.setPixel(x, y, if (bm[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        ivQrGen.setImageBitmap(bmp)
    }
}
