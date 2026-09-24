"""
VidGrab – yt-dlp download engine.

Called from Kotlin via Chaquopy. Accepts a URL, output directory,
FFmpeg binary path, and a Kotlin callback object for progress updates.
"""

import os
import yt_dlp


def download_video(url: str, output_dir: str, ffmpeg_dir: str, callback=None):
    """
    Download the best-quality video+audio from *url*, merge with FFmpeg,
    and save to *output_dir*.

    Args:
        url:        The video URL (YouTube, etc.)
        output_dir: Absolute path to write the final file.
        ffmpeg_dir: Absolute path to the directory containing ffmpeg/ffprobe.
        callback:   Optional Kotlin object with onProgress(msg) / onFinished(path).

    Returns:
        The video title as a string.
    """

    def _progress_hook(d):
        """Relay yt-dlp progress events back to the Android UI."""
        if callback is None:
            return

        status = d.get("status", "")

        if status == "downloading":
            pct = d.get("_percent_str", "??%").strip()
            speed = d.get("_speed_str", "").strip()
            eta = d.get("_eta_str", "").strip()
            msg = f"Downloading… {pct}  {speed}  ETA {eta}"
            callback.onProgress(msg)

        elif status == "finished":
            callback.onProgress("Merging audio + video…")

    def _postprocessor_hook(d):
        """Fires when ffmpeg finishes merging."""
        if callback and d.get("status") == "finished":
            filepath = d.get("info_dict", {}).get("filepath", "")
            callback.onFinished(filepath)

    ydl_opts = {
        # ── Quality ──────────────────────────────────────────
        "format": "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
        "merge_output_format": "mp4",

        # ── Paths ────────────────────────────────────────────
        "outtmpl": os.path.join(output_dir, "%(title)s.%(ext)s"),
        "ffmpeg_location": ffmpeg_dir,

        # ── Behaviour ────────────────────────────────────────
        "quiet": True,
        "no_warnings": True,
        "noprogress": False,       # keep progress_hooks alive
        "overwrites": True,

        # ── Hooks ────────────────────────────────────────────
        "progress_hooks": [_progress_hook],
        "postprocessor_hooks": [_postprocessor_hook],
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        title = info.get("title", "video")
        return title
