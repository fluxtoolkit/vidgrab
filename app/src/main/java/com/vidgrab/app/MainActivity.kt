package com.vidgrab.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.vidgrab.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Main (and only) screen of VidGrab.
 *
 * Flow:
 *  1. User pastes a URL or shares one from another app.
 *  2. Tap "Download" → launches a coroutine on [Dispatchers.IO].
 *  3. The coroutine calls [downloader.py] via Chaquopy, passing a
 *     [DownloadCallback] object so Python can push progress updates
 *     back to the UI thread.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isDownloading = false

    // ─────────────────────────── Lifecycle ───────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDownload.setOnClickListener { onDownloadClicked() }

        // Pre-fill URL if the user shared a link to VidGrab
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Handles the ACTION_SEND intent so users can tap "Share" in
     * YouTube / Chrome and send the URL directly to VidGrab.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                binding.etUrl.setText(sharedText)
            }
        }
    }

    // ─────────────────────────── Download ────────────────────────────

    private fun onDownloadClicked() {
        val url = binding.etUrl.text?.toString()?.trim().orEmpty()

        if (url.isBlank()) {
            binding.tilUrl.error = "Please paste a video URL"
            return
        }
        binding.tilUrl.error = null

        if (isDownloading) {
            Toast.makeText(this, "A download is already running", Toast.LENGTH_SHORT).show()
            return
        }

        startDownload(url)
    }

    private fun startDownload(url: String) {
        isDownloading = true
        setUiDownloading(true)
        updateStatus("Preparing…")

        lifecycleScope.launch(Dispatchers.IO) {

            // 1. Ensure FFmpeg binaries are extracted and executable
            val ffmpegDir = FFmpegHelper.getFFmpegDir(this@MainActivity)

            // 2. Prepare the output directory
            //    Downloads/VidGrab/ — visible in the file manager, no permissions
            //    needed on Android 10+ scoped storage.
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
                "VidGrab"
            )
            if (!outputDir.exists()) outputDir.mkdirs()

            // 3. Build the Kotlin ↔ Python callback bridge
            val callback = DownloadCallback()

            // 4. Invoke the Python yt-dlp engine
            try {
                val py: Python = Python.getInstance()
                val module: PyObject = py.getModule("downloader")

                val title: PyObject = module.callAttr(
                    "download_video",
                    url,
                    outputDir.absolutePath,
                    ffmpegDir,
                    callback
                )

                withContext(Dispatchers.Main) {
                    updateStatus("✅  Done — ${title.toString()}")
                    Toast.makeText(
                        this@MainActivity,
                        "Saved to Downloads/VidGrab/",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    updateStatus("❌  Error: $msg")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    setUiDownloading(false)
                }
            }
        }
    }

    // ─────────────── Callback object passed into Python ─────────────

    /**
     * This object is passed to Python as `callback`.
     * Python calls `callback.onProgress(msg)` and `callback.onFinished(path)`
     * via Chaquopy's automatic Java ↔ Python proxy bridge.
     *
     * Inner class so it can call [runOnUiThread] on the enclosing Activity.
     */
    inner class DownloadCallback {

        /** Called from Python on each yt-dlp progress tick. */
        fun onProgress(message: String) {
            runOnUiThread { updateStatus(message) }
        }

        /** Called from Python when post-processing (FFmpeg merge) is done. */
        fun onFinished(filepath: String) {
            runOnUiThread {
                updateStatus("✅  Saved: ${File(filepath).name}")
            }
        }
    }

    // ──────────────────────── UI Helpers ─────────────────────────────

    private fun setUiDownloading(active: Boolean) {
        binding.progressBar.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnDownload.isEnabled = !active
        binding.btnDownload.text = if (active) "Downloading…" else "Download"
    }

    private fun updateStatus(msg: String) {
        binding.tvStatus.text = msg
    }
}
