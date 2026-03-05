package com.ruuvi.station.tagsettings.ui.led_control

import com.ruuvi.station.R

sealed interface LedBrightnessLevel {
    val commandLevel: String
    val titleRes: Int
    object Bright: LedBrightnessLevel {
        override val commandLevel: String
            get() = "bright_day"
        override val titleRes: Int
            get() = R.string.led_level_3
    }

    object Normal: LedBrightnessLevel {
        override val commandLevel: String
            get() = "day"
        override val titleRes: Int
            get() = R.string.led_level_2
    }

    object Dim: LedBrightnessLevel {
        override val commandLevel: String
            get() = "night"
        override val titleRes: Int
            get() = R.string.led_level_1
    }

    object Off: LedBrightnessLevel {
        override val commandLevel: String
            get() = "off"
        override val titleRes: Int
            get() = R.string.led_level_0
    }

    companion object {
        fun getAllLevels(): List<LedBrightnessLevel> = listOf(
            Bright,
            Normal,
            Dim,
            Off,
        )
    }
}