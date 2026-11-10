package info.dvkr.screenstream.common.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import info.dvkr.screenstream.common.module.StreamingModule
import kotlinx.coroutines.flow.StateFlow

public interface AppSettings {

    public object Key {
        public val STREAMING_MODULE: Preferences.Key<String> = stringPreferencesKey("STREAMING_MODULE")
        public val NIGHT_MODE: Preferences.Key<Int> = intPreferencesKey("NIGHT_MODE")
        public val DYNAMIC_THEME: Preferences.Key<Boolean> = booleanPreferencesKey("DYNAMIC_THEME")
        public val SCREEN_CAPTURE_EDUCATION_COMPLETED: Preferences.Key<Boolean> = booleanPreferencesKey("SCREEN_CAPTURE_EDUCATION_COMPLETED")
        public val START_ON_BOOT: Preferences.Key<Boolean> = booleanPreferencesKey("START_ON_BOOT")
        public val AUTO_START_STREAM: Preferences.Key<Boolean> = booleanPreferencesKey("AUTO_START_STREAM")
        public val ENABLE_CAMERA_OVERLAY: Preferences.Key<Boolean> = booleanPreferencesKey("ENABLE_CAMERA_OVERLAY")
    }

    public object Default {
        public val STREAMING_MODULE: StreamingModule.Id = StreamingModule.Id("_NONE_")
        public const val NIGHT_MODE: Int = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        public const val DYNAMIC_THEME: Boolean = false
        public const val SCREEN_CAPTURE_EDUCATION_COMPLETED: Boolean = false
        public const val START_ON_BOOT: Boolean = true
        public const val AUTO_START_STREAM: Boolean = true
        public const val ENABLE_CAMERA_OVERLAY: Boolean = false
    }

    @Immutable
    public data class Data(
        public val streamingModule: StreamingModule.Id = Default.STREAMING_MODULE,
        public val nightMode: Int = Default.NIGHT_MODE,
        public val dynamicTheme: Boolean = Default.DYNAMIC_THEME,
        public val screenCaptureEducationCompleted: Boolean = Default.SCREEN_CAPTURE_EDUCATION_COMPLETED,
        public val startOnBoot: Boolean = Default.START_ON_BOOT,
        public val autoStartStream: Boolean = Default.AUTO_START_STREAM,
        public val enableCameraOverlay: Boolean = Default.ENABLE_CAMERA_OVERLAY,
    )

    public val data: StateFlow<Data>
    public suspend fun updateData(transform: Data.() -> Data)
}
