package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun TextEditButton(
    value: String?,
    emptyText: String,
    editAction: () -> Unit
) {
    Row(modifier = Modifier
        .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight)
        .clickable { editAction.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = if (value.isNullOrEmpty()) emptyText else value)
        Image(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            painter = painterResource(id = R.drawable.edit_20),
            contentDescription = ""
        )
    }
}

@Composable
fun TextEditWithCaptionButton(
    value: String? = null,
    title: String,
    icon: Painter,
    tint: Color,
    editAction: () -> Unit
) {
    Row(modifier = Modifier
        .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight)
        .clickable { editAction.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.subtitle,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value ?: "")
        Icon(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            painter = icon,
            tint = tint,
            contentDescription = ""
        )
    }
}

@Composable
fun TextWithCaption(
    value: String? = null,
    title: String
) {
    Row(modifier = Modifier
        .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.subtitle,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value ?: "")
    }
}