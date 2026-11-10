package info.dvkr.screenstream.ui.tabs.settings.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.dvkr.screenstream.R
import info.dvkr.screenstream.ui.tabs.settings.app.common.SettingActionRow
import info.dvkr.screenstream.ui.tabs.settings.app.common.SettingSwitchRow

@Composable
internal fun CameraOverlayRow(
    enableCameraOverlay: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingSwitchRow(
        checked = enableCameraOverlay,
        iconRes = R.drawable.stream_24px,
        title = stringResource(id = R.string.app_pref_camera_overlay),
        summary = stringResource(id = R.string.app_pref_camera_overlay_summary),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
internal fun CameraPermissionRow(
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingActionRow(
        iconRes = R.drawable.stream_24px,
        title = stringResource(id = R.string.app_pref_camera_permission),
        summary = stringResource(
            id = if (isGranted) R.string.app_pref_camera_permission_summary_on
            else R.string.app_pref_camera_permission_summary_off
        ),
        onClick = onClick,
        modifier = modifier
    )
}

