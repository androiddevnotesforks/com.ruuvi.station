package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme


@Composable
fun RuuviConfirmDialog(
    title: String = "",
    message: String = "",
    onDismissRequest : () -> Unit,
    onYesClickAction: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.extended)
                .fillMaxWidth(),
            shape = RoundedCornerShape(RuuviStationTheme.dimensions.medium),
            backgroundColor = RuuviStationTheme.colors.background
        )
        {
            Column(
                modifier = Modifier
                    .padding(all = RuuviStationTheme.dimensions.extended)
            ) {
                if (title.isNotEmpty()) {
                    SubtitleWithPadding(text = title)
                }

                if (message.isNotEmpty()) {
                    ParagraphWithPadding(text = message)
                }

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    RuuviTextButton(
                        text = stringResource(id = R.string.no),
                        onClick = {
                            onDismissRequest.invoke()
                        }
                    )

                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.extended))

                    RuuviTextButton(
                        text = stringResource(id = R.string.yes),
                        onClick = {
                            onYesClickAction.invoke()
                        }
                    )
                }
            }
        }
    }
}