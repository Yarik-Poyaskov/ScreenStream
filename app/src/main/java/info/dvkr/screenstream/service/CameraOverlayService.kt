package info.dvkr.screenstream.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.R
import info.dvkr.screenstream.common.getLog

public class CameraOverlayService : Service() {

    internal companion object {
        private const val NOTIFICATION_CHANNEL_ID = "camera_overlay_channel"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_START = "info.dvkr.screenstream.action.START_CAMERA_OVERLAY"
        private const val ACTION_STOP = "info.dvkr.screenstream.action.STOP_CAMERA_OVERLAY"

        public fun start(context: Context) {
            val intent = Intent(context, CameraOverlayService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                XLog.e(getLog("CameraOverlayService", "Failed to startForegroundService: ${t.message}"))
            }
        }

        public fun stop(context: Context) {
            val intent = Intent(context, CameraOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (t: Throwable) {
                XLog.e(getLog("CameraOverlayService", "Failed to stopService: ${t.message}"))
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var textureView: TextureView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startOverlay()
            ACTION_STOP -> stopOverlay()
            else -> stopOverlay()
        }
        return START_NOT_STICKY
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            XLog.w(getLog("CameraOverlayService", "canDrawOverlays is false, stopping"))
            stopSelf()
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            XLog.w(getLog("CameraOverlayService", "CAMERA permission not granted, stopping"))
            stopSelf()
            return
        }

        startForegroundNotification()
        startBackgroundThread()
        showOverlayView()
    }

    private fun startForegroundNotification() {
        val channelName = getString(R.string.app_pref_camera_overlay)
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.app_pref_camera_overlay_summary)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_pref_camera_overlay))
            .setContentText(getString(R.string.app_pref_camera_overlay_summary))
            .setSmallIcon(R.drawable.ic_notification_small_24dp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraOverlayThread").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (_: InterruptedException) {
        }
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayView() {
        if (overlayContainer != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        val widthPx = (150 * density).toInt()
        val heightPx = (200 * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - widthPx - (16 * density).toInt()).coerceAtLeast(0)
            y = (48 * density).toInt()
        }
        windowLayoutParams = params

        val container = FrameLayout(this).apply {
            val cornerRadius = 16f * density
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
            clipToOutline = true
            setBackgroundColor(0xFF202124.toInt())
            elevation = 10f * density
        }

        val texture = TextureView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openCamera(surface)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            }
        }
        container.addView(texture)
        textureView = texture

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    runCatching { windowManager?.updateViewLayout(container, params) }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(container, params)
            overlayContainer = container
        } catch (t: Throwable) {
            XLog.e(getLog("CameraOverlayService", "Failed to addView to WindowManager: ${t.message}"))
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(surfaceTexture: SurfaceTexture) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (frontCameraId == null) {
                XLog.w(getLog("CameraOverlayService", "No available camera found"))
                return
            }

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCaptureSession(camera, surfaceTexture)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    XLog.e(getLog("CameraOverlayService", "CameraDevice error: $error"))
                }
            }, backgroundHandler)
        } catch (t: Throwable) {
            XLog.e(getLog("CameraOverlayService", "Failed to open camera: ${t.message}"))
        }
    }

    private fun startCaptureSession(camera: CameraDevice, surfaceTexture: SurfaceTexture) {
        try {
            val surface = Surface(surfaceTexture)
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            @Suppress("DEPRECATION")
            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    } catch (t: Throwable) {
                        XLog.e(getLog("CameraOverlayService", "Failed to setRepeatingRequest: ${t.message}"))
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    XLog.e(getLog("CameraOverlayService", "CameraCaptureSession configure failed"))
                }
            }, backgroundHandler)
        } catch (t: Throwable) {
            XLog.e(getLog("CameraOverlayService", "Failed to createCaptureSession: ${t.message}"))
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
        } catch (_: Throwable) {}
        captureSession = null

        try {
            cameraDevice?.close()
        } catch (_: Throwable) {}
        cameraDevice = null
    }

    private fun stopOverlay() {
        closeCamera()
        stopBackgroundThread()

        overlayContainer?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Throwable) {}
            overlayContainer = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }
}

