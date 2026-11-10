package info.dvkr.screenstream.common.ui.mediaprojection

public object ScreenCaptureAutoConfirmState {
    @Volatile
    public var isWaitingForSystemPermission: Boolean = false

    @Volatile
    public var autoStartConsumedForSession: Boolean = false

    public fun resetAutoStartForBoot() {
        autoStartConsumedForSession = false
    }
}

