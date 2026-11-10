package info.dvkr.screenstream.ui.tabs.settings.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.dvkr.screenstream.R
import info.dvkr.screenstream.ui.tabs.settings.app.common.SettingActionRow
import info.dvkr.screenstream.ui.tabs.settings.app.common.SettingSwitchRow

@Composable
internal fun StartOnBootRow(
    startOnBoot: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingSwitchRow(
        checked = startOnBoot,
        iconRes = R.drawable.stream_24px,
        title = stringResource(id = R.string.app_pref_start_on_boot),
        summary = stringResource(id = R.string.app_pref_start_on_boot_summary),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
internal fun AutoStartStreamRow(
    autoStartStream: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingSwitchRow(
        checked = autoStartStream,
        iconRes = R.drawable.stream_24px,
        title = stringResource(id = R.string.app_pref_auto_start_stream),
        summary = stringResource(id = R.string.app_pref_auto_start_stream_summary),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
internal fun OverlayPermissionRow(
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingActionRow(
        iconRes = R.drawable.tile_small_24px,
        title = stringResource(id = R.string.app_pref_overlay_permission),
        summary = stringResource(
            id = if (isGranted) R.string.app_pref_overlay_permission_summary_on
            else R.string.app_pref_overlay_permission_summary_off
        ),
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun AutoConfirmAccessibilityRow(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingActionRow(
        iconRes = R.drawable.settings_24px,
        title = stringResource(id = R.string.app_pref_auto_confirm_accessibility),
        summary = stringResource(
            id = if (isEnabled) R.string.app_pref_auto_confirm_accessibility_summary_on
            else R.string.app_pref_auto_confirm_accessibility_summary
        ),
        onClick = onClick,
        modifier = modifier
    )
}
