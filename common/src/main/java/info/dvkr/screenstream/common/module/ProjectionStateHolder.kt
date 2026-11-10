package info.dvkr.screenstream.common.module

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public object ProjectionStateHolder {
    private val _isProjecting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isProjecting: StateFlow<Boolean> = _isProjecting.asStateFlow()

    public fun setProjecting(value: Boolean) {
        _isProjecting.value = value
    }
}

