package com.example.lexd

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lexd.databinding.ActivityMainBinding
import com.example.lexd.network.DownloadRequest
import com.example.lexd.network.RetrofitClient
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val modeOptions = listOf(
        "🎬 Video + Ses (MP4)" to "video_audio",
        "📽️ Sessiz Video (MP4)" to "video_only",
        "🎧 Sadece Ses (MP3/M4A)" to "audio_only"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.modeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            modeOptions.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.downloadButton.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            if (url.isEmpty()) {
                binding.urlInput.error = "URL girmeniz gerekiyor"
                return@setOnClickListener
            }

            val selectedMode = modeOptions[binding.modeSpinner.selectedItemPosition].second
            startDownload(url, selectedMode)
        }
    }

    private fun startDownload(url: String, mode: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = RetrofitClient.service.download(DownloadRequest(url, mode))
                val fullUrl = RetrofitClient.BASE_URL + resp.file_url

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val req = DownloadManager.Request(Uri.parse(fullUrl))
                    .setTitle(resp.filename)
                    .setDescription("LEXD indiriliyor")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resp.filename)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setMimeType(
                        when {
                            resp.filename.endsWith(".mp3") || resp.filename.endsWith(".m4a") -> "audio/m4a"
                            else -> "video/mp4"
                        }
                    )

                dm.enqueue(req)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "İndirme başladı!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}