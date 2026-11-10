package info.dvkr.screenstream.service

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.module.ProjectionStateHolder
import info.dvkr.screenstream.common.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

public object CameraOverlayController {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isInitialized: Boolean = false

    public fun init(context: Context, appSettings: AppSettings) {
        if (isInitialized) return
        isInitialized = true

        combine(
            ProjectionStateHolder.isProjecting,
            appSettings.data
        ) { isProjecting, settings ->
            isProjecting && settings.enableCameraOverlay
        }
        .distinctUntilChanged()
        .onEach { shouldShow ->
            XLog.i(getLog("CameraOverlayController", "shouldShow=$shouldShow"))
            if (shouldShow) {
                startOverlay(context)
            } else {
                stopOverlay(context)
            }
        }
        .launchIn(scope)
    }

    private fun startOverlay(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            XLog.w(getLog("CameraOverlayController", "Overlay permission not granted. Cannot start camera overlay."))
            return
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            XLog.w(getLog("CameraOverlayController", "Camera permission not granted. Cannot start camera overlay."))
            return
        }
        CameraOverlayService.start(context)
    }

    private fun stopOverlay(context: Context) {
        CameraOverlayService.stop(context)
    }
}

