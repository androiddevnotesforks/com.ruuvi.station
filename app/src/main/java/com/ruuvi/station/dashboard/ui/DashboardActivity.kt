package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
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
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.ui.MyAccountActivity
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.network.ui.claim.ClaimSensorActivity
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

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val dashboardViewModel: DashboardActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        observeSyncStatus()

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
                val dashboardType by dashboardViewModel.dashboardType.collectAsState()

                Scaffold(
                    scaffoldState = scaffoldState,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                    topBar = { DashboardTopAppBar(
                        actionCallBack = { dashboardType -> dashboardViewModel.changeDashboardType(dashboardType) },
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
                        DashboardItems(sensors, userEmail, dashboardType)
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

    private fun observeSyncStatus() {
        lifecycleScope.launchWhenStarted {
            dashboardViewModel.syncEvents.collect {
                if (it is NetworkSyncEvent.Unauthorised) {
                    dashboardViewModel.signOut()
                    SignInActivity.start(this@DashboardActivity)
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
fun DashboardItems(items: List<RuuviTag>, userEmail: String?, dashboardType: DashboardType) {
    val configuration = LocalConfiguration.current
    val itemHeight = 156.dp * LocalDensity.current.fontScale
    val imageWidth = configuration.screenWidthDp.dp * 0.24f

    Timber.d("Image width $imageWidth ${configuration.screenWidthDp.dp}")
    LazyColumn() {
        items(items, key = {it.id}) { sensor ->
            when (dashboardType) {
                DashboardType.SIMPLE_VIEW -> DashboardItemSimple(
                    sensor = sensor,
                    userEmail = userEmail
                )
                DashboardType.IMAGE_VIEW ->
                    DashboardItem(
                        imageWidth = imageWidth,
                        itemHeight = itemHeight,
                        sensor = sensor,
                        userEmail = userEmail
                    )
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun DashboardItem(imageWidth: Dp, itemHeight: Dp, sensor: RuuviTag, userEmail: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .height(itemHeight)
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

                if (sensor.userBackground != null) {
                    val uri = Uri.parse(sensor.userBackground)

                    if (uri.path != null) {
                        DashboardImage(uri)
                    }
                }
            }

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = RuuviStationTheme.dimensions.mediumPlus,
                        start = RuuviStationTheme.dimensions.mediumPlus,
                        bottom = RuuviStationTheme.dimensions.medium,
                        end = RuuviStationTheme.dimensions.medium,
                    )
            ) {
                val (name, temp, buttons, updated, column1, column2) = createRefs()

                Text(
                    style = RuuviStationTheme.typography.title,
                    text = sensor.displayName,
                    lineHeight = RuuviStationTheme.fontSizes.extended,
                    fontSize = RuuviStationTheme.fontSizes.normal,
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
                    modifier = Modifier
                        .constrainAs(temp) {
                            top.linkTo(name.bottom)
                            start.linkTo(parent.start)
                        }
                        .offset(y = (-8).dp * LocalDensity.current.fontScale),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        style = RuuviStationTheme.typography.dashboardTemperature,
                        text = sensor.temperatureValue.valueWithoutUnit,
                        lineHeight = 10.sp,
                        color = tempTextColor
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                top = 8.dp * LocalDensity.current.fontScale,
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
                        bottom.linkTo(updated.top)
                        end.linkTo(column2.start)
                        width = Dimension.fillToConstraints
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
                        bottom.linkTo(updated.top)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
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
                        .padding(top = 2.dp)
                        .constrainAs(updated) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                        },
                    fontSize = RuuviStationTheme.fontSizes.smallest
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun DashboardItemSimple(sensor: RuuviTag, userEmail: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            //.height(itemHeight)
            .fillMaxWidth()
            .clickable {
                TagDetailsActivity.start(context, sensor.id)
            },
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = RuuviStationTheme.colors.dashboardCardBackground
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = RuuviStationTheme.dimensions.mediumPlus,
                    start = RuuviStationTheme.dimensions.mediumPlus,
                    bottom = RuuviStationTheme.dimensions.medium,
                    end = RuuviStationTheme.dimensions.medium,
                )
        ) {
            val (name, buttons, updated, column1, column2) = createRefs()

            Text(
                style = RuuviStationTheme.typography.title,
                text = sensor.displayName,
                lineHeight = RuuviStationTheme.fontSizes.extended,
                fontSize = RuuviStationTheme.fontSizes.normal,
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

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.constrainAs(column1) {
                    top.linkTo(name.bottom)
                    start.linkTo(parent.start)
                    //bottom.linkTo(column2.bottom)
                    end.linkTo(column2.start)
                    //height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                }
            ) {
                ValueDisplay(
                    value = sensor.temperatureValue,
                    sensor.status.triggered(AlarmType.TEMPERATURE)
                )
                if (sensor.humidityValue != null) {
                    ValueDisplay(
                        value = sensor.humidityValue,
                        sensor.status.triggered(AlarmType.HUMIDITY)
                    )
                }
                if (sensor.pressureValue != null) {
                    ValueDisplay(
                        value = sensor.pressureValue,
                        sensor.status.triggered(AlarmType.PRESSURE)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.constrainAs(column2) {
                    start.linkTo(column1.end)
                    top.linkTo(name.bottom)
                    //bottom.linkTo(column1.bottom)
                    end.linkTo(parent.end)
                    //height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                }
            ) {
                ValueDisplay(value = sensor.voltageValue, false)

                if (sensor.movementValue != null) {
                    ValueDisplay(
                        value = sensor.movementValue,
                        sensor.status.triggered(AlarmType.MOVEMENT)
                    )
                }
            }

            Text(
                style = RuuviStationTheme.typography.paragraph,
                text = sensor.updatedAt?.describingTimeSince(LocalContext.current) ?: "",
                modifier = Modifier
                    .padding(top = 2.dp)
                    .constrainAs(updated) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(column1.bottom)
                        width = Dimension.fillToConstraints
                    },
                fontSize = RuuviStationTheme.fontSizes.smallest
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun DashboardItemVariableHeight(imageWidth: Dp, itemHeight: Dp, sensor: RuuviTag, userEmail: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            //.height(itemHeight)
            .fillMaxWidth()
            .clickable {
                TagDetailsActivity.start(context, sensor.id)
            },
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = RuuviStationTheme.colors.dashboardCardBackground
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val (image, values) = createRefs()

            Box(
                Modifier
                    //.width(imageWidth)
                    .fillMaxSize()
                    .background(color = RuuviStationTheme.colors.defaultSensorBackground)
                    .constrainAs(image) {
                        top.linkTo(values.top)
                        bottom.linkTo(values.bottom)
                        start.linkTo(parent.start)
                        height = Dimension.fillToConstraints
                        width = Dimension.value(imageWidth)
                    }
            ) {
                Timber.d("Image path ${sensor.userBackground} ")

                if (sensor.userBackground != null) {
                    val uri = Uri.parse(sensor.userBackground)

                    if (uri.path != null) {
                        GlideImage(
                            modifier = Modifier.fillMaxSize(),
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                        //DashboardImage(uri)
                    }
                }
            }

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = RuuviStationTheme.dimensions.mediumPlus,
                        start = RuuviStationTheme.dimensions.mediumPlus,
                        bottom = RuuviStationTheme.dimensions.medium,
                        end = RuuviStationTheme.dimensions.medium,
                    )
                    .constrainAs(values) {
                        start.linkTo(image.end)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        width = Dimension.fillToConstraints
                        height = Dimension.wrapContent
                    }
            ) {
                val (name, temp, buttons, updated, column1, column2) = createRefs()

                Text(
                    style = RuuviStationTheme.typography.title,
                    text = sensor.displayName,
                    lineHeight = RuuviStationTheme.fontSizes.extended,
                    fontSize = RuuviStationTheme.fontSizes.normal,
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
                    modifier = Modifier
                        .constrainAs(temp) {
                            top.linkTo(name.bottom)
                            start.linkTo(parent.start)
                        },
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        style = RuuviStationTheme.typography.dashboardTemperature,
                        text = sensor.temperatureValue.valueWithoutUnit,
                        lineHeight = 10.sp,
                        color = tempTextColor
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                top = 8.dp * LocalDensity.current.fontScale,
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
                        end.linkTo(column2.start)
                        top.linkTo(temp.bottom)
                        bottom.linkTo(column2.bottom)
                        width = Dimension.fillToConstraints
                        //height = Dimension.fillToConstraints
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
                        end.linkTo(parent.end)
                        top.linkTo(temp.bottom)
                        bottom.linkTo(column1.bottom)
                        width = Dimension.fillToConstraints
                        //height = Dimension.fillToConstraints
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
                        .padding(top = 2.dp)
                        .constrainAs(updated) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(column1.bottom)
                            width = Dimension.fillToConstraints
                        },
                    fontSize = RuuviStationTheme.fontSizes.smallest
                )
            }

        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DashboardImage(userBackground: Uri) {
    Timber.d("Image path $userBackground")
    GlideImage(
        modifier = Modifier.fillMaxSize(),
        model = userBackground,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = R.drawable.tag_bg_layer),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ValueDisplay(value: EnvironmentValue, alertTriggered: Boolean) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlert
    } else {
        RuuviStationTheme.colors.primary
    }

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value.valueWithoutUnit,
            style = RuuviStationTheme.typography.dashboardValue,
            color = textColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(width = 4.dp))
        Text(
            text = value.unitString,
            style = RuuviStationTheme.typography.dashboardUnit,
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