# FFmpeg Binaries for VidGrab

This directory must contain **statically compiled** `ffmpeg` and `ffprobe`
binaries, renamed with a `lib` prefix and `.so` extension so Android's build
system packages them automatically.

## Required structure

```
jniLibs/
├── arm64-v8a/                ← physical ARM64 devices
│   ├── libffmpeg.so
│   └── libffprobe.so
└── x86_64/                   ← Android Emulator (optional)
    ├── libffmpeg.so
    └── libffprobe.so
```

## Where to get the binaries

1. **Recommended**: [hzw1199/Android-FFmpeg-Prebuilt](https://github.com/hzw1199/Android-FFmpeg-Prebuilt)
   - Provides CLI executables compiled for arm64-v8a
   - Compatible with Android 16KB page sizes
   - Download `ffmpeg` and `ffprobe` from the releases page

2. **Alternative**: [Tyrrrz/FFmpegBin](https://github.com/Tyrrrz/FFmpegBin/releases)
   - Automated static builds for multiple platforms including Android arm64

## Steps

1. Download the `ffmpeg` and `ffprobe` arm64 binaries
2. Rename them:
   - `ffmpeg`  → `libffmpeg.so`
   - `ffprobe` → `libffprobe.so`
3. Place them in `jniLibs/arm64-v8a/`
4. (Optional) Repeat for `x86_64` if you test on emulator
5. Rebuild the project

The app's `FFmpegHelper.kt` will extract these at runtime and make them
executable for yt-dlp.
