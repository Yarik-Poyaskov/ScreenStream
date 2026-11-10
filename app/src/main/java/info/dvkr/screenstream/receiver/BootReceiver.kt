package info.dvkr.screenstream.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.SingleActivity
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.common.ui.mediaprojection.ScreenCaptureAutoConfirmState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val appSettings: AppSettings by inject(mode = LazyThreadSafetyMode.NONE)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        XLog.i(getLog("onReceive", "Action: $action"))

        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            if (!appSettings.data.value.startOnBoot) {
                XLog.i(getLog("onReceive", "startOnBoot is disabled. Skipping launch."))
                return
            }

            ScreenCaptureAutoConfirmState.resetAutoStartForBoot()

            runCatching {
                val startIntent = SingleActivity.getBootAutoStartIntent(context)
                context.startActivity(startIntent)
                XLog.i(getLog("onReceive", "Launched SingleActivity for boot auto-start"))
            }.onFailure { cause ->
                XLog.e(getLog("onReceive", "Failed to start SingleActivity on boot: ${cause.message}"), cause)
            }
        }
    }
}

