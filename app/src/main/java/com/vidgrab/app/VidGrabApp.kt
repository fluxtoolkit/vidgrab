package com.vidgrab.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

/**
 * Application subclass that initializes the Chaquopy Python runtime once,
 * before any Activity starts. Declared in AndroidManifest as android:name.
 */
class VidGrabApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start the embedded Python interpreter (only once per process)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
