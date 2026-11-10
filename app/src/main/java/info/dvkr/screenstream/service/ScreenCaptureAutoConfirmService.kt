package info.dvkr.screenstream.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.ui.mediaprojection.ScreenCaptureAutoConfirmState

public class ScreenCaptureAutoConfirmService : AccessibilityService() {

    internal companion object {
        private val ENTIRE_SCREEN_KEYWORDS = listOf(
            "entire screen",
            "весь экран",
            "весь екран",
            "увесь екран",
            "whole screen",
            "full screen"
        )

        private val SINGLE_APP_KEYWORDS = listOf(
            "single app",
            "a single app",
            "одно приложение",
            "одна програма",
            "один додаток"
        )

        internal fun isServiceEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, ScreenCaptureAutoConfirmService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(':')
                .mapNotNull { ComponentName.unflattenFromString(it) }
                .any { it == expectedComponentName }
        }
    }

    private var spinnerOpenedForCurrentAttempt: Boolean = false
    private var entireScreenSelectedForCurrentAttempt: Boolean = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!ScreenCaptureAutoConfirmState.isWaitingForSystemPermission) {
            spinnerOpenedForCurrentAttempt = false
            entireScreenSelectedForCurrentAttempt = false
            return
        }

        val rootNode = rootInActiveWindow ?: event?.source ?: return
        val pkgName = rootNode.packageName?.toString().orEmpty()
        if (!pkgName.contains("systemui", ignoreCase = true) &&
            !pkgName.contains("permission", ignoreCase = true) &&
            !pkgName.contains("smartcapture", ignoreCase = true) &&
            !pkgName.contains("media", ignoreCase = true)
        ) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                handleAndroid14PlusDialog(rootNode)
            } else {
                clickConfirmButton(rootNode)
            }
        } catch (cause: Throwable) {
            XLog.w(getLog("onAccessibilityEvent", "Failed to auto-confirm: ${cause.message}"), cause)
        }
    }

    private fun handleAndroid14PlusDialog(rootNode: AccessibilityNodeInfo) {
        // 1. If the dropdown list of options is currently visible, select "Entire screen" (or index 1)
        if (!entireScreenSelectedForCurrentAttempt) {
            val entireScreenOption = findEntireScreenDropdownOption(rootNode)
            if (entireScreenOption != null) {
                XLog.i(getLog("handleAndroid14PlusDialog", "Selecting 'Entire screen' option in dropdown"))
                if (clickNodeOrParent(entireScreenOption)) {
                    entireScreenSelectedForCurrentAttempt = true
                    return
                }
            }
        }

        // 2. Check if there is a mode selector spinner on the dialog
        val spinnerNode = findNodeByViewId(rootNode, "com.android.systemui:id/screen_share_mode_options")
            ?: findNodeByViewId(rootNode, "com.android.systemui:id/screen_share_mode_spinner")

        if (spinnerNode != null && !entireScreenSelectedForCurrentAttempt) {
            val spinnerText = collectNodeText(spinnerNode).lowercase()
            val alreadyEntireScreen = ENTIRE_SCREEN_KEYWORDS.any { spinnerText.contains(it) }
            val isSingleApp = SINGLE_APP_KEYWORDS.any { spinnerText.contains(it) }

            if (alreadyEntireScreen && !isSingleApp) {
                entireScreenSelectedForCurrentAttempt = true
            } else if (!spinnerOpenedForCurrentAttempt) {
                XLog.i(getLog("handleAndroid14PlusDialog", "Opening screen share mode spinner"))
                if (clickNodeOrParent(spinnerNode)) {
                    spinnerOpenedForCurrentAttempt = true
                    return
                }
            }
        }

        // 3. Click the positive confirmation button ("Start now" / "Начать")
        clickConfirmButton(rootNode)
    }

    private fun findEntireScreenDropdownOption(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for a node matching "Entire screen" / "Весь экран" that is inside a ListView/DropDownListView or clickable
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        var listViewSecondItem: AccessibilityNodeInfo? = null

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val className = node.className?.toString().orEmpty()
            val viewId = node.viewIdResourceName.orEmpty()

            if (className.contains("ListView", ignoreCase = true) || className.contains("RecyclerView", ignoreCase = true)) {
                if (node.childCount >= 2) {
                    listViewSecondItem = node.getChild(1)
                }
            }

            if (!viewId.endsWith("screen_share_mode_options") && !viewId.endsWith("screen_share_mode_spinner")) {
                val text = node.text?.toString()?.lowercase().orEmpty()
                if (text.isNotEmpty() && ENTIRE_SCREEN_KEYWORDS.any { text.contains(it) }) {
                    if (node.isClickable || node.parent?.isClickable == true) {
                        return node
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return listViewSecondItem
    }

    private fun clickConfirmButton(rootNode: AccessibilityNodeInfo): Boolean {
        val confirmButton = findNodeByViewId(rootNode, "android:id/button1")
            ?: findNodeByViewId(rootNode, "com.android.systemui:id/button1")
            ?: return false

        if (confirmButton.isEnabled) {
            XLog.i(getLog("clickConfirmButton", "Clicking positive button (android:id/button1)"))
            return clickNodeOrParent(confirmButton)
        }
        return false
    }

    private fun findNodeByViewId(rootNode: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? =
        rootNode.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()

    private fun collectNodeText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append(' ') }
        node.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { sb.append(collectNodeText(it)).append(' ') }
        }
        return sb.toString()
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    override fun onInterrupt() {
        XLog.d(getLog("onInterrupt"))
    }
}

