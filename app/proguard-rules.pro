# Add any project specific ProGuard rules here.
# Chaquopy and yt-dlp don't require special ProGuard rules
# since they use JNI rather than reflection.

# Keep the download callback class accessible from Python
-keep class com.vidgrab.app.MainActivity$DownloadCallback { *; }
