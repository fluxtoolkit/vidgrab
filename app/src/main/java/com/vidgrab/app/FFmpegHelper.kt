package com.vidgrab.app

import android.content.Context
import java.io.File

/**
 * Extracts the bundled ffmpeg/ffprobe binaries from jniLibs (.so files)
 * to the app's internal files directory and makes them executable.
 *
 * The binaries are stored in jniLibs as lib{name}.so so that the Android
 * build system packages them automatically per ABI. At runtime the system
 * unpacks them into [Context.getApplicationInfo().nativeLibraryDir]. We
 * copy them to a clean directory so yt-dlp can find them by name.
 */
object FFmpegHelper {

    private const val BIN_DIR_NAME = "ffmpeg_bin"

    /**
     * @return Absolute path to the directory containing the `ffmpeg` and
     *         `ffprobe` executables, ready for yt-dlp's `ffmpeg_location`.
     */
    fun getFFmpegDir(context: Context): String {
        val dir = File(context.filesDir, BIN_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()

        val binaries = listOf("ffmpeg", "ffprobe")
        for (name in binaries) {
            val target = File(dir, name)
            if (!target.exists() || !target.canExecute()) {
                // The system unpacks jniLibs/<abi>/lib<name>.so here automatically
                val source = File(
                    context.applicationInfo.nativeLibraryDir,
                    "lib${name}.so"
                )
                if (source.exists()) {
                    source.copyTo(target, overwrite = true)
                    target.setExecutable(true, false)
                }
            }
        }
        return dir.absolutePath
    }
}
