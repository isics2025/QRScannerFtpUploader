package com.example.qrftpapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanContract
import org.apache.commons.net.ftp.FTPClient
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { uploadFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = com.example.qrftpapp.databinding.ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanButton.setOnClickListener {
            val options = ScanOptions().apply {
                captureActivity = CaptureActivity::class.java
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                prompt = "Scan a QR code"
            }
            barcodeLauncher.launch(options)
        }
    }

    private fun uploadFile(filePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ftpClient = FTPClient()
                ftpClient.connect("your.ftp.server")
                ftpClient.login("username", "password")
                val file = File(filePath)
                file.inputStream().use { input ->
                    ftpClient.storeFile(file.name, input)
                }
                ftpClient.logout()
                ftpClient.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
