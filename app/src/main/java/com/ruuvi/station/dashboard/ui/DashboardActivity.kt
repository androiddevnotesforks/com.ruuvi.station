package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.ui.DashboardTopAppBar
import com.ruuvi.station.app.ui.MainMenu
import com.ruuvi.station.app.ui.MenuItem
import com.ruuvi.station.app.ui.components.BlinkingEffect
import com.ruuvi.station.app.ui.components.Title
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviStationTheme.typography
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.network.ui.ClaimSensorActivity
import com.ruuvi.station.network.ui.MyAccountActivity
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.SettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.util.extensions.describingTimeSince
import com.ruuvi.station.util.extensions.openUrl
import com.ruuvi.station.util.extensions.sendFeedback
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber
import java.io.File
import java.io.FileInputStream

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val dashboardViewModel: DashboardActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme {
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val context = LocalContext.current
                val isDarkTheme = isSystemInDarkTheme()
                val scope = rememberCoroutineScope()
                val userEmail by dashboardViewModel.userEmail.observeAsState()
                val signedIn = !userEmail.isNullOrEmpty()
                val sensors by dashboardViewModel.tagsFlow.collectAsState(initial = listOf())

                Scaffold(
                    scaffoldState = scaffoldState,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                    topBar = { DashboardTopAppBar(
                        actionCallBack = { AddTagActivity.start(context) },
                        navigationCallback = {
                            scope.launch {
                                scaffoldState.drawerState.open()
                            }
                        }
                    ) },
                    drawerContent = {
                                    MainMenu(
                                        items = listOf(
                                            MenuItem(R.string.menu_add_new_sensor, stringResource(id = R.string.menu_add_new_sensor)),
                                            MenuItem(R.string.menu_app_settings, stringResource(id = R.string.menu_app_settings)),
                                            MenuItem(R.string.menu_about_help, stringResource(id = R.string.menu_about_help)),
                                            MenuItem(R.string.menu_send_feedback, stringResource(id = R.string.menu_send_feedback)),
                                            MenuItem(R.string.menu_what_to_measure, stringResource(id = R.string.menu_what_to_measure)),
                                            MenuItem(R.string.menu_buy_sensors, stringResource(id = R.string.menu_buy_sensors)),
                                            MenuItem(R.string.menu_buy_gateway, stringResource(id = R.string.menu_buy_gateway)),
                                            if (signedIn) {
                                                MenuItem(R.string.my_ruuvi_account, stringResource(id = R.string.my_ruuvi_account))
                                            } else {
                                                MenuItem(R.string.sign_in, stringResource(id = R.string.sign_in))
                                            }
                                        ),
                                        onItemClick = { item ->
                                            when (item.id) {
                                                R.string.menu_add_new_sensor -> AddTagActivity.start(context)
                                                R.string.menu_app_settings -> SettingsActivity.start(context)
                                                R.string.menu_about_help -> AboutActivity.start(context)
                                                R.string.menu_send_feedback -> sendFeedback()
                                                R.string.menu_what_to_measure -> openUrl(getString(R.string.what_to_measure_link))
                                                R.string.menu_buy_sensors -> openUrl(getString(R.string.buy_sensors_link))
                                                R.string.menu_buy_gateway -> openUrl(getString(R.string.buy_gateway_link))
                                                R.string.my_ruuvi_account -> MyAccountActivity.start(context)
                                                R.string.sign_in -> SignInActivity.start(context)
                                            }
                                            scope.launch {
                                                scaffoldState.drawerState.close()
                                            }
                                        }
                                    )
                                    },
                    drawerBackgroundColor = RuuviStationTheme.colors.background
                ) { paddingValues ->
                    Surface(
                        color = RuuviStationTheme.colors.dashboardBackground,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(RuuviStationTheme.dimensions.medium)
                    ) {
                        DashboardItems(sensors, userEmail)
                    }
                }

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor,
                        darkIcons = !isDarkTheme
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val dashboardIntent = Intent(context, DashboardActivity::class.java)
            context.startActivity(dashboardIntent)
        }
    }
}

@Composable
fun DashboardItems(items: List<RuuviTag>, userEmail: String?) {
    val configuration = LocalConfiguration.current
    val imageWidth = configuration.screenWidthDp.dp * 0.27f
    Timber.d("Image width $imageWidth ${configuration.screenWidthDp.dp}")
    LazyColumn() {
        items(items) { sensor ->
            DashboardItem(imageWidth, sensor, userEmail)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardItem(imageWidth: Dp, sensor: RuuviTag, userEmail: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
            .clickable {
                TagDetailsActivity.start(context, sensor.id)
            },
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = RuuviStationTheme.colors.dashboardCardBackground
    ) {
        //Image(painter = , contentDescription = )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(imageWidth)
                    .fillMaxHeight()
                    .background(color = RuuviStationTheme.colors.defaultSensorBackground)
            ) {
                Timber.d("Image path ${sensor.userBackground} ")

                var file: File? = null
                if (sensor.userBackground?.isNotEmpty() == true) {
                    val uri = Uri.parse(sensor.userBackground)
                    uri.path?.let {
                        file = File(it)
                    }
                    Timber.d("File ${file?.absolutePath} ${file?.exists()}")
                    Timber.d("Image path ${sensor.userBackground} uri ${Uri.parse(sensor.userBackground)}")

                }

                if (file?.exists() == true) {
                    val bitmap = BitmapFactory.decodeStream(FileInputStream(file))
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "", contentScale = ContentScale.Crop)

                }
                else {
                    Image(
                        painter = painterResource(id = R.drawable.tag_bg_layer),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        alpha = if (isSystemInDarkTheme()) 0.75f else 0.30f
                    )
                }
            }

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = RuuviStationTheme.dimensions.extended,
                        start = RuuviStationTheme.dimensions.extended,
                        bottom = RuuviStationTheme.dimensions.medium,
                        end = RuuviStationTheme.dimensions.medium,
                    )
            ) {
                val (name, temp, buttons, updated, column1, column2) = createRefs()

                Title(
                    text = sensor.displayName,
                    modifier = Modifier.constrainAs(name) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(buttons.start)
                        width = Dimension.fillToConstraints
                    },
                    maxLines = 2
                )

                Row(
                    modifier = Modifier
                        .width(72.dp)
                        .constrainAs(buttons) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.End,
                ) {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        if (sensor.status is AlarmSensorStatus.NotTriggered) {
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = {
                                    TagSettingsActivity.start(context, sensor.id)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_notifications_on_24px),
                                    contentDescription = null,
                                    tint = RuuviStationTheme.colors.accent
                                )
                            }
                        } else if (sensor.status is AlarmSensorStatus.Triggered) {
                            BlinkingEffect() {
                                IconButton(
                                    modifier = Modifier.size(36.dp),
                                    onClick = {
                                        TagSettingsActivity.start(context, sensor.id)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_notifications_active_24px),
                                        contentDescription = null,
                                        tint = RuuviStationTheme.colors.activeAlert
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }

                        DashboardItemDropdownMenu(sensor, userEmail)
                    }
                }

                val tempTextColor = if (sensor.status.triggered(AlarmType.TEMPERATURE)) {
                    RuuviStationTheme.colors.activeAlert
                } else {
                    RuuviStationTheme.colors.settingsTitleText
                }

                Row(
                    modifier = Modifier.constrainAs(temp){
                        top.linkTo(name.bottom)
                        start.linkTo(parent.start)
                    },
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        style = RuuviStationTheme.typography.dashboardTemperature,
                        text = sensor.temperatureValue.valueWithoutUnit,
                        color = tempTextColor
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                top = 8.dp,
                                start = 2.dp
                            ),
                        style = RuuviStationTheme.typography.dashboardTemperatureUnit,
                        text = sensor.temperatureValue.unitString,
                        color = tempTextColor
                    )
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.constrainAs(column1) {
                        start.linkTo(parent.start)
                        top.linkTo(temp.bottom)
                        bottom.linkTo(updated.top)
                        end.linkTo(column2.start)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                ) {
                    if (sensor.humidityValue != null) {
                        ValueDisplay(value = sensor.humidityValue, sensor.status.triggered(AlarmType.HUMIDITY))
                    }
                    if (sensor.pressureValue != null) {
                        ValueDisplay(value = sensor.pressureValue, sensor.status.triggered(AlarmType.PRESSURE))
                    }
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.constrainAs(column2) {
                        start.linkTo(column1.end)
                        top.linkTo(temp.bottom)
                        bottom.linkTo(updated.top)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                ) {
                    ValueDisplay(value = sensor.voltageValue, false)

                    if (sensor.movementValue != null) {
                        ValueDisplay(value = sensor.movementValue, sensor.status.triggered(AlarmType.MOVEMENT))
                    }
                }

                Text(
                    style = RuuviStationTheme.typography.paragraph,
                    text = sensor.updatedAt?.describingTimeSince(LocalContext.current) ?: "",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .constrainAs(updated) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                        },
                    fontSize = RuuviStationTheme.fontSizes.tiny
                )
            }
        }
    }
}

@Composable
fun ValueDisplay(value: EnvironmentValue, alertTriggered: Boolean) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlert
    } else {
        RuuviStationTheme.colors.settingsTitleText
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value.valueWithoutUnit,
            style = typography.dashboardValue,
            color = textColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(width = 4.dp))
        Text(
            text = value.unitString,
            style = typography.dashboardUnit,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun DashboardItemDropdownMenu(
    sensor: RuuviTag,
    userEmail: String?
) {
    val context = LocalContext.current
    var threeDotsMenuExpanded by remember {
        mutableStateOf(false)
    }

    val canBeShared = sensor.owner == userEmail
    val canBeClaimed = sensor.owner.isNullOrEmpty() && userEmail?.isNotEmpty() == true

    Box() {
        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { threeDotsMenuExpanded = !threeDotsMenuExpanded }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_3dots),
                tint = RuuviStationTheme.colors.dashboardBurger,
                contentDescription = ""
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = RuuviStationTheme.colors.background),
            expanded = threeDotsMenuExpanded,
            onDismissRequest = { threeDotsMenuExpanded = false }
        ) {
            DropdownMenuItem(onClick = {
                TagDetailsActivity.start(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.full_image_view
                ))
            }

            DropdownMenuItem(onClick = {
                TagDetailsActivity.start(context, sensor.id, true)
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.history_view
                ))
            }
            DropdownMenuItem(onClick = {
                TagSettingsActivity.start(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.settings_and_alerts
                ))
            }
            DropdownMenuItem(onClick = {
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.change_background
                ))
            }

            if (canBeClaimed) {
                DropdownMenuItem(onClick = {
                    ClaimSensorActivity.start(context, sensor.id)
                    threeDotsMenuExpanded = false
                }) {
                    com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                        id = R.string.claim_sensor
                    ))
                }
            } else if (canBeShared) {
                DropdownMenuItem(onClick = {
                    ShareSensorActivity.start(context, sensor.id)
                    threeDotsMenuExpanded = false
                }) {
                    com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                        id = R.string.share
                    ))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
}