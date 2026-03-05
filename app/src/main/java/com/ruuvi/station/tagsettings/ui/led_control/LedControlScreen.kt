package com.ruuvi.station.tagsettings.ui.led_control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.RadioButtonRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun LedControlScreen(
    viewModel: LedControlViewModel,
    modifier: Modifier = Modifier
) {
    val selectedLevel by viewModel.level.collectAsStateWithLifecycle()

    Column (
        modifier = modifier
            .fillMaxSize()
            .padding(RuuviStationTheme.dimensions.screenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        if (viewModel.canChangeSettings) {
            LedLevelsRadioGroup(
                selectedLevel = selectedLevel,
                onSelect = viewModel::selectBrightnessLevel
            )
        } else {
            Paragraph(stringResource(R.string.led_brightness_fw_update_message))
        }
    }
}

@Composable
fun LedLevelsRadioGroup(
    selectedLevel: LedBrightnessLevel?,
    onSelect: (LedBrightnessLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = remember { LedBrightnessLevel.getAllLevels() }
    Paragraph(stringResource(R.string.led_brightness_select_message))
    Spacer(Modifier.height(8.dp))
    Column(modifier = modifier) {
        levels.forEach { level ->
            val isSelected = selectedLevel == level

            RadioButtonRuuvi(
                text = stringResource(id = level.titleRes),
                isSelected = isSelected,
                onClick = { onSelect(level) }
            )
        }
    }

}