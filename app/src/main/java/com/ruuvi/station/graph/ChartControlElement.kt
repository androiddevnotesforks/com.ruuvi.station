package com.ruuvi.station.graph

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.tagdetails.ui.SyncStatus
import com.ruuvi.station.util.Period
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@Composable
fun ChartControlElement2(
    sensorId: String,
    viewPeriod: Period,
    syncStatus: Flow<SyncStatus>,
    disconnectGattAction: (String) -> Unit,
    shouldSkipGattSyncDialog: () -> Boolean,
    syncGatt: (String) -> Unit,
    setViewPeriod: (Int) -> Unit,
    exportToCsv: (String) -> Uri?,
    removeTagData: (String) -> Unit,
    refreshStatus: () -> Unit,
    dontShowGattSyncDescription: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val context = LocalContext.current

    var gattSyncDialogOpened by remember {
        mutableStateOf(false)
    }

    var syncInProgress by remember {
        mutableStateOf(false)
    }

    var syncMessage by remember {
        mutableStateOf<UiText>(UiText.EmptyString)
    }

    var uiEvent by remember {
        mutableStateOf<SyncProgress?>(null)
    }

    LaunchedEffect(key1 = true) {
        syncStatus.collect() { event ->
            Timber.d("SyncEvent collected $event")

            if (listOf(
                    SyncProgress.ERROR,
                    SyncProgress.NOT_FOUND,
                    SyncProgress.NOT_SUPPORTED,
                    SyncProgress.DISCONNECTED
                ).contains(event.syncProgress)
            ) {
                uiEvent = event.syncProgress
            }
            syncInProgress = event.syncInProgress
            syncMessage = event.statusMessage
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (syncInProgress) {
            IconButton(onClick = {
                disconnectGattAction(sensorId)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_clear_24),
                    contentDescription = null,
                    tint = RuuviStationTheme.colors.buttonText
                )
            }
            Text(
                style = RuuviStationTheme.typography.syncStatusText,
                text = syncMessage.asString(context)
            )
        } else {
            IconButton(onClick = {
                if (!shouldSkipGattSyncDialog()) {
                    gattSyncDialogOpened = true
                }
                syncGatt(sensorId)
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = RuuviStationTheme.dimensions.extended)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.gatt_sync),
                        contentDescription = null,
                        tint = RuuviStationTheme.colors.buttonText
                    )
                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
                    Text(
                        style = RuuviStationTheme.typography.subtitle,
                        text = stringResource(id = R.string.sync),
                        color = RuuviStationTheme.colors.buttonText
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            ViewPeriodMenu(
                viewPeriod = viewPeriod,
                setViewPeriod = setViewPeriod
            )

            ThreeDotsMenu(
                sensorId = sensorId,
                exportToCsv = { exportToCsv(sensorId) },
                clearHistory = { removeTagData(sensorId) }
            )
        }
    }

    DisposableEffect( key1 = lifecycleOwner ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    if (uiEvent != null) {
        when (uiEvent) {
            SyncProgress.DISCONNECTED -> {
                RuuviMessageDialog(message = stringResource(id = R.string.disconnected)) {
                    uiEvent = null
                }
            }
            SyncProgress.NOT_SUPPORTED -> {
                RuuviMessageDialog(message = stringResource(id = R.string.reading_history_not_supported)) {
                    uiEvent = null
                }
            }
            SyncProgress.NOT_FOUND -> {
                if (!gattSyncDialogOpened) {
                    RuuviConfirmDialog(
                        title = stringResource(id = R.string.error),
                        message = stringResource(id = R.string.gatt_not_in_range_description),
                        noButtonCaption = stringResource(id = R.string.close),
                        yesButtonCaption = stringResource(id = R.string.try_again),
                        onDismissRequest = {
                            uiEvent = null
                        }
                    ) {
                        uiEvent = null
                        syncGatt(sensorId)
                    }
                }
            }
            SyncProgress.ERROR -> {
                RuuviMessageDialog(message = stringResource(id = R.string.something_went_wrong)) {
                    uiEvent = null
                }
            }
            else -> {
                uiEvent = null
            }
        }
    }

    if (gattSyncDialogOpened) {
        GattSyncDescriptionDialog(
            doNotShowAgain = {
                gattSyncDialogOpened = false
                dontShowGattSyncDescription()
            },
            onDismissRequest = {
                gattSyncDialogOpened = false
            }
        )
    }
}

@Composable
fun ViewPeriodMenu(
    viewPeriod: Period,
    setViewPeriod: (Int) -> Unit
) {
    var daysMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showMoreDialog by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.clickable { daysMenuExpanded = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val daysText = if (viewPeriod.shouldPassValue) {
                stringResource(id = viewPeriod.stringResourceId, viewPeriod.value)
            } else {
                stringResource(id = viewPeriod.stringResourceId)
            }
            Text(
                style = RuuviStationTheme.typography.subtitle,
                text = daysText,
                color = RuuviStationTheme.colors.buttonText
            )

            Icon(
                painter = painterResource(id = R.drawable.drop_down_24),
                contentDescription = "",
                tint = RuuviStationTheme.colors.accent
            )
        }
        DropdownMenu(
            modifier = Modifier.background(color = RuuviStationTheme.colors.background),
            expanded = daysMenuExpanded,
            onDismissRequest = { daysMenuExpanded = false }) {
            val periodOptions = listOf(
                Period.All(),
                Period.Hour1(),
                Period.Hour2(),
                Period.Hour3(),
                Period.Hour12(),
                Period.Day1(),
                Period.Day2(),
                Period.Day3(),
                Period.Day4(),
                Period.Day5(),
                Period.Day6(),
                Period.Day7(),
                Period.Day8(),
                Period.Day9(),
                Period.Day10(),
            )
            for (day in periodOptions) {
                DropdownMenuItem(onClick = {
                    setViewPeriod.invoke(day.value)
                    daysMenuExpanded = false
                }) {
                    Paragraph(text = stringResource(id = day.stringResourceId))
                }
            }

            DropdownMenuItem(onClick = {
                showMoreDialog = true
                daysMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.more))
            }
        }
    }
    
    if (showMoreDialog) {
        RuuviMessageDialog(
            title = stringResource(id = R.string.longer_history_title),
            message = stringResource(id = R.string.longer_history_message)
        ) {
            showMoreDialog = false
        }
    }
}

@Composable
fun ThreeDotsMenu(
    sensorId: String,
    exportToCsv: () -> Uri?,
    clearHistory: () -> Unit,
) {
    val context = LocalContext.current
    var clearConfirmOpened by remember {
        mutableStateOf(false)
    }
    var threeDotsMenuExpanded by remember {
        mutableStateOf(false)
    }

    Box() {
        IconButton(onClick = { threeDotsMenuExpanded = !threeDotsMenuExpanded }) {
            Icon(painter = painterResource(id = R.drawable.ic_3dots),
                contentDescription = null,
                tint = RuuviStationTheme.colors.buttonText
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = RuuviStationTheme.colors.background),
            expanded = threeDotsMenuExpanded,
            onDismissRequest = { threeDotsMenuExpanded = false }
        ) {
            DropdownMenuItem(onClick = {
                val uri = exportToCsv.invoke()
                if (uri != null) { sendCsv(sensorId, uri, context) }
                threeDotsMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.export_history))
            }
            DropdownMenuItem(onClick = {
                clearConfirmOpened = true
                threeDotsMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.clear_view))
            }
        }
    }

    if (clearConfirmOpened) {
        ClearHistoryDialog(
            clearHistory = clearHistory,
            dismissAction = { clearConfirmOpened = false }
        )
    }
}

@Composable
fun GattSyncDescriptionDialog(
    onDismissRequest : () -> Unit,
    doNotShowAgain: () -> Unit,
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
                SubtitleWithPadding(text = stringResource(id = R.string.synchronisation))

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                
                ParagraphWithPadding(text = stringResource(id = R.string.gatt_sync_description))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RuuviTextButton(
                        text = stringResource(id = R.string.do_not_show_again),
                        onClick = {
                            doNotShowAgain.invoke()
                        }
                    )

                    RuuviTextButton(
                        text = stringResource(id = R.string.close),
                        onClick = {
                            onDismissRequest.invoke()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
            }
        }
    }
}

@Composable
fun ClearHistoryDialog(
    clearHistory: () -> Unit,
    dismissAction: () -> Unit
) {
    RuuviConfirmDialog(
        title = stringResource(id = R.string.clear_local_history),
        message = stringResource(id = R.string.clear_local_history_description),
        yesButtonCaption = stringResource(id = R.string.clear),
        noButtonCaption = stringResource(id = R.string.cancel),
        onDismissRequest = { dismissAction.invoke() }
    ) {
        dismissAction.invoke()
        clearHistory.invoke()
    }
}

fun sendCsv(sensorId: String, uri: Uri, context: Context) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sendIntent.type = "text/csv"
    sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_csv_chooser_title, sensorId)))
}