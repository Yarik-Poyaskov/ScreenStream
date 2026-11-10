package info.dvkr.screenstream.ui.tabs.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import info.dvkr.screenstream.InlineAdaptiveBanner
import info.dvkr.screenstream.R
import info.dvkr.screenstream.common.notification.NotificationHelper
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.service.ScreenCaptureAutoConfirmService
import info.dvkr.screenstream.tile.TileActionService
import info.dvkr.screenstream.ui.tabs.settings.app.AppLocaleDetail
import info.dvkr.screenstream.ui.tabs.settings.app.AppLocaleRow
import info.dvkr.screenstream.ui.tabs.settings.app.AutoConfirmAccessibilityRow
import info.dvkr.screenstream.ui.tabs.settings.app.AutoStartStreamRow
import info.dvkr.screenstream.ui.tabs.settings.app.CameraOverlayRow
import info.dvkr.screenstream.ui.tabs.settings.app.CameraPermissionRow
import info.dvkr.screenstream.ui.tabs.settings.app.DynamicThemeRow
import info.dvkr.screenstream.ui.tabs.settings.app.NightModeDetail
import info.dvkr.screenstream.ui.tabs.settings.app.NightModeRow
import info.dvkr.screenstream.ui.tabs.settings.app.NotificationsRow
import info.dvkr.screenstream.ui.tabs.settings.app.OverlayPermissionRow
import info.dvkr.screenstream.ui.tabs.settings.app.StartOnBootRow
import info.dvkr.screenstream.ui.tabs.settings.app.TileRow
import info.dvkr.screenstream.ui.theme.dynamicThemeAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class AppSetting { APP_LOCALE, NIGHT_MODE }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsTabContent(
    boundsInWindow: Rect,
    modifier: Modifier = Modifier,
    appSettings: AppSettings = koinInject(),
    notificationHelper: NotificationHelper = koinInject(),
    scope: CoroutineScope = rememberCoroutineScope()
) {
    val settingsData = appSettings.data.collectAsStateWithLifecycle().value

    val context = LocalContext.current
    val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val accessibilityEnabled = remember { mutableStateOf(ScreenCaptureAutoConfirmService.isServiceEnabled(context)) }
    val cameraGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraGranted.value = isGranted
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted.value = Settings.canDrawOverlays(context)
        accessibilityEnabled.value = ScreenCaptureAutoConfirmService.isServiceEnabled(context)
        cameraGranted.value = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val listPanePreferredWidth = calculateListPanePreferredWidth(windowAdaptiveInfo, boundsInWindow)
    val scaffoldDirective = calculatePaneScaffoldDirective(windowAdaptiveInfo, HingePolicy.AvoidOccluding)
        .copy(verticalPartitionSpacerSize = 0.dp, horizontalPartitionSpacerSize = 0.dp)
    val navigator = rememberListDetailPaneScaffoldNavigator<AppSetting>(scaffoldDirective)
    val lazyListState = rememberLazyListState()

    BackHandler(enabled = navigator.canNavigateBack()) { scope.launch { navigator.navigateBack() } }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(listPanePreferredWidth)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        state = lazyListState
                    ) {
                        item(key = "SETTINGS_HEADER", contentType = "HEADER") {
                            Text(
                                text = stringResource(id = R.string.app_pref_settings),
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        item(key = "START_ON_BOOT", contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                StartOnBootRow(
                                    startOnBoot = settingsData.startOnBoot,
                                    onValueChange = { startOnBoot ->
                                        if (settingsData.startOnBoot != startOnBoot) {
                                            scope.launch { appSettings.updateData { copy(startOnBoot = startOnBoot) } }
                                        }
                                    }
                                )
                            }
                        }

                        item(key = "AUTO_START_STREAM", contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                HorizontalDivider()
                                AutoStartStreamRow(
                                    autoStartStream = settingsData.autoStartStream,
                                    onValueChange = { autoStartStream ->
                                        if (settingsData.autoStartStream != autoStartStream) {
                                            scope.launch { appSettings.updateData { copy(autoStartStream = autoStartStream) } }
                                        }
                                    }
                                )
                            }
                        }

                        item(key = "CAMERA_OVERLAY", contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                HorizontalDivider()
                                CameraOverlayRow(
                                    enableCameraOverlay = settingsData.enableCameraOverlay,
                                    onValueChange = { enableCameraOverlay ->
                                        if (settingsData.enableCameraOverlay != enableCameraOverlay) {
                                            scope.launch { appSettings.updateData { copy(enableCameraOverlay = enableCameraOverlay) } }
                                        }
                                        if (enableCameraOverlay && !cameraGranted.value) {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }
                                )
                            }
                        }

                        if (settingsData.enableCameraOverlay && !cameraGranted.value) {
                            item(key = "CAMERA_PERMISSION", contentType = "ITEM") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    HorizontalDivider()
                                    CameraPermissionRow(
                                        isGranted = cameraGranted.value,
                                        onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) }
                                    )
                                }
                            }
                        }

                        item(key = "OVERLAY_PERMISSION", contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                HorizontalDivider()
                                OverlayPermissionRow(
                                    isGranted = overlayGranted.value,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        item(key = "AUTO_CONFIRM_ACCESSIBILITY", contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                HorizontalDivider()
                                AutoConfirmAccessibilityRow(
                                    isEnabled = accessibilityEnabled.value,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }
                                    }
                                )
                            }
                        }

                        item(key = AppSetting.APP_LOCALE, contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                HorizontalDivider()
                                AppLocaleRow(
                                    onShowDetail = { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, AppSetting.APP_LOCALE) } })
                            }
                        }

                        item(key = AppSetting.NIGHT_MODE, contentType = "ITEM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                HorizontalDivider()
                                NightModeRow(
                                    nightMode = settingsData.nightMode,
                                    onShowDetail = { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, AppSetting.NIGHT_MODE) } })
                            }
                        }

                        if (dynamicThemeAvailable) {
                            item(key = "DYNAMIC_THEME", contentType = "ITEM") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    HorizontalDivider()
                                    DynamicThemeRow(
                                        dynamicTheme = settingsData.dynamicTheme,
                                        onValueChange = { dynamicTheme ->
                                            if (settingsData.dynamicTheme != dynamicTheme) {
                                                scope.launch { appSettings.updateData { copy(dynamicTheme = dynamicTheme) } }
                                            }
                                        })
                                }
                            }
                        }

                        if (notificationHelper.canOpenAppNotificationSettings()) {
                            item(key = "NOTIFICATIONS", contentType = "ITEM") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    HorizontalDivider()
                                    NotificationsRow(onClick = { context.startActivity(notificationHelper.getNotificationSettingsIntent()) })
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            item(key = "TILE", contentType = "ITEM") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    HorizontalDivider()
                                    TileRow(onClick = { TileActionService.showAddTileRequest(context) })
                                }
                            }
                        }

                        item(key = "AD_BANNER", contentType = "AD") {
                            InlineAdaptiveBanner(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                when (navigator.currentDestination?.contentKey) {
                    AppSetting.APP_LOCALE -> AppLocaleDetail(
                        headerContent = { title -> DetailUITitle(title, navigator.canNavigateBack()) { scope.launch { navigator.navigateBack() } } })

                    AppSetting.NIGHT_MODE -> NightModeDetail(
                        headerContent = { title -> DetailUITitle(title, navigator.canNavigateBack()) { scope.launch { navigator.navigateBack() } } },
                        nightMode = settingsData.nightMode,
                        onNightModeSelected = { nightMode ->
                            if (settingsData.nightMode != nightMode) {
                                scope.launch { appSettings.updateData { copy(nightMode = nightMode) } }
                            }
                        })

                    else -> Unit
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun DetailUITitle(title: String, canNavigateBack: Boolean, navigateBack: () -> Unit) {
    if (canNavigateBack) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(id = R.string.app_pref_back)
                )
            }
            Text(
                text = title,
                modifier = Modifier.padding(start = 16.dp),
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Text(
            text = title,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun calculateListPanePreferredWidth(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    boundsInWindow: Rect
): Dp {
    if (boundsInWindow.isEmpty) return 360.dp
    val hinge = windowAdaptiveInfo.windowPosture.hingeList.firstOrNull()
    return when {
        hinge == null || hinge.isFlat ->
            when {
                windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
                    -> with(LocalDensity.current) { (boundsInWindow.width / 2).toDp() }

                else -> 360.dp
            }

        hinge.isVertical -> with(LocalDensity.current) { (hinge.bounds.left - boundsInWindow.left).toDp() } // BookPosture
        else -> 360.dp
    }
}
