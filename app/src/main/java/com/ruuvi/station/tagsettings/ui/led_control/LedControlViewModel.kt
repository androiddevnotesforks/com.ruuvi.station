package com.ruuvi.station.tagsettings.ui.led_control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.bluetooth.domain.air.AirLedBrightnessController
import com.ruuvi.station.database.domain.SensorSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer

class LedControlViewModel(
    val sensorId: String,
    val sensorSettingsRepository: SensorSettingsRepository,
    val ledController: AirLedBrightnessController
): ViewModel() {

    val canChangeSettings: Boolean
        get() = canControlLed()

    private val _level = MutableStateFlow<LedBrightnessLevel?> (null)
    val level: StateFlow<LedBrightnessLevel?> = _level

    private fun canControlLed(): Boolean {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val version = sensorSettings?.firmware?.trim()
            ?.let { raw ->
                val start = raw.indexOfFirst { it.isDigit() }.takeIf { it >= 0 } ?: return@let null
                val candidate = raw.substring(start)
                runCatching { SemVer.parse(candidate) }.getOrNull()
            }
            ?: return false

        return version >= MIN_VERSION
    }

    fun selectBrightnessLevel(level: LedBrightnessLevel) {
        viewModelScope.launch {
            _level.value = level
            ledController.setBrightness(sensorId, level)
        }
    }

    companion object {
        private val MIN_VERSION = SemVer.parse("1.0.7")
    }
}