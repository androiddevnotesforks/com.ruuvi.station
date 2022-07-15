package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun PageSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = RuuviStationTheme.colors.background,
        modifier = modifier
            .fillMaxSize()
    ) {
        content.invoke()
    }
}

@Composable
fun PageSurfaceWithPadding(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PageSurface(modifier.padding(RuuviStationTheme.dimensions.screenPadding)) {
        content.invoke()
    }
}