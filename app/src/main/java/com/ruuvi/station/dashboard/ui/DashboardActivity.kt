package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.permissions.BluetoothPermissions
import com.ruuvi.station.app.permissions.NotificationPermission
import com.ruuvi.station.app.ui.DashboardTopAppBar
import com.ruuvi.station.app.ui.MainMenu
import com.ruuvi.station.app.ui.MenuItem
import com.ruuvi.station.app.ui.components.BlinkingEffect
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.components.rememberResourceUri
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviStationTheme.colors
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.ui.MyAccountActivity
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.network.ui.claim.ClaimSensorActivity
import com.ruuvi.station.settings.ui.SettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.isLowBattery
import com.ruuvi.station.tagdetails.ui.NfcInteractor
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.tagsettings.ui.BackgroundActivity
import com.ruuvi.station.tagsettings.ui.SetSensorName
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.util.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val dashboardViewModel: DashboardActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        observeSyncStatus()

        setContent {
            var bluetoothCheckReady by remember {
                mutableStateOf(false)
            }

            RuuviTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val configuration = LocalConfiguration.current
                val context = LocalContext.current
                val isDarkTheme = isSystemInDarkTheme()
                val scope = rememberCoroutineScope()
                val userEmail by dashboardViewModel.userEmail.observeAsState()
                val signedIn = !userEmail.isNullOrEmpty()
                val sensors by remember(dashboardViewModel.tagsFlow, lifecycleOwner) {
                    dashboardViewModel.tagsFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                }.collectAsState(null)
                val refreshing by dashboardViewModel.dataRefreshing.collectAsState(false)
                val dashboardType by dashboardViewModel.dashboardType.collectAsState()
                val dashboardTapAction by dashboardViewModel.dashboardTapAction.collectAsState()

                NotificationPermission(
                    scaffoldState = scaffoldState,
                    shouldAskNotificationPermission = dashboardViewModel.shouldAskNotificationPermission
                ) {
                    bluetoothCheckReady = true
                }

                if (bluetoothCheckReady) {
                    BluetoothPermissions(
                        scaffoldState = scaffoldState,
                        askToEnableBluetooth = dashboardViewModel.shouldAskToEnableBluetooth,
                        askForBackgroundLocation = dashboardViewModel.shouldAskForBackgroundLocationPermission
                    )
                }

                Box (
                    modifier =
                        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            Modifier
                                .statusBarsPadding()
                                .windowInsetsPadding(WindowInsets.tappableElement)
                        } else {
                            Modifier.statusBarsPadding()
                        }
                ) {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                        topBar = {
                            DashboardTopAppBar(
                                dashboardType = dashboardType,
                                dashboardTapAction = dashboardTapAction,
                                changeDashboardType = dashboardViewModel::changeDashboardType,
                                changeDashboardTapAction = dashboardViewModel::changeDashboardTapAction,
                                navigationCallback = {
                                    scope.launch {
                                        scaffoldState.drawerState.open()
                                    }
                                }
                            )
                        },
                        drawerContent = {
                            MainMenu(
                                items = listOf(
                                    MenuItem(
                                        R.string.menu_add_new_sensor,
                                        stringResource(id = R.string.menu_add_new_sensor)
                                    ),
                                    MenuItem(
                                        R.string.menu_app_settings,
                                        stringResource(id = R.string.menu_app_settings)
                                    ),
                                    MenuItem(
                                        R.string.menu_about_help,
                                        stringResource(id = R.string.menu_about_help)
                                    ),
                                    MenuItem(
                                        R.string.menu_send_feedback,
                                        stringResource(id = R.string.menu_send_feedback)
                                    ),
                                    MenuItem(
                                        R.string.menu_what_to_measure,
                                        stringResource(id = R.string.menu_what_to_measure)
                                    ),
                                    MenuItem(
                                        R.string.menu_buy_sensors,
                                        stringResource(id = R.string.menu_buy_sensors)
                                    ),
                                    if (signedIn) {
                                        MenuItem(
                                            R.string.my_ruuvi_account,
                                            stringResource(id = R.string.my_ruuvi_account)
                                        )
                                    } else {
                                        MenuItem(
                                            R.string.sign_in,
                                            stringResource(id = R.string.sign_in)
                                        )
                                    }
                                ),
                                onItemClick = { item ->
                                    when (item.id) {
                                        R.string.menu_add_new_sensor -> AddTagActivity.start(context)
                                        R.string.menu_app_settings -> SettingsActivity.start(context)
                                        R.string.menu_about_help -> AboutActivity.start(context)
                                        R.string.menu_send_feedback -> sendFeedback()
                                        R.string.menu_what_to_measure -> openUrl(getString(R.string.what_to_measure_link))
                                        R.string.menu_buy_sensors -> openUrl(getString(R.string.buy_sensors_menu_link))
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
                                .padding(paddingValues)
                        ) {
                            sensors?.let {
                                if (it.isEmpty()) {
                                    EmptyDashboard() {
                                        openUrl(getString(R.string.buy_sensors_link))
                                    }
                                } else {
                                    DashboardItems(
                                        items = it,
                                        userEmail = userEmail,
                                        dashboardType = dashboardType,
                                        dashboardTapAction = dashboardTapAction,
                                        syncCloud = dashboardViewModel::syncCloud,
                                        setName = dashboardViewModel::setName,
                                        refreshing = refreshing
                                    )
                                }
                            }
                        }
                    }
                }

                NfcInteractor(
                    getNfcScanResponse = dashboardViewModel::getNfcScanResponse,
                    addSensor = dashboardViewModel::addSensor
                )

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = systemBarsColor,
                        darkIcons = !isDarkTheme
                    )
                    systemUiController.setNavigationBarColor(
                        color = Color.Transparent,
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

                if (it is NetworkSyncEvent.Success) {
                    dashboardViewModel.refreshDashboardType()
                    dashboardViewModel.refreshDashboardTapAction()
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val dashboardIntent = Intent(context, DashboardActivity::class.java)
            dashboardIntent
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(dashboardIntent)
        }
    }
}

@Composable
fun EmptyDashboard(buySensors: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = RuuviStationTheme.dimensions.medium,
                end = RuuviStationTheme.dimensions.medium,
                bottom = RuuviStationTheme.dimensions.medium,
            )
            .systemBarsPadding(),
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = RuuviStationTheme.colors.dashboardCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
                style = RuuviStationTheme.typography.onboardingSubtitle,
                color = colors.primary,
                text = stringResource(id = R.string.dashboard_no_sensors_message),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
            RuuviButton(text = stringResource(id = R.string.add_your_first_sensor)) {
                AddTagActivity.start(context)
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
            Text(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.extended)
                    .clickable { buySensors.invoke() },
                style = RuuviStationTheme.typography.title,
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.menu_buy_sensors),
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardItems(
    items: List<RuuviTag>,
    userEmail: String?,
    dashboardType: DashboardType,
    dashboardTapAction: DashboardTapAction,
    syncCloud: ()-> Unit,
    setName: (String, String?) -> Unit,
    refreshing: Boolean
) {
    val itemHeight = 156.dp * LocalDensity.current.fontScale

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            syncCloud.invoke()
        }
    )

    val pullToRefreshModifier = if (userEmail.isNullOrEmpty()) {
        Modifier
    } else {
        Modifier.pullRefresh(pullRefreshState)
    }

    Box(modifier = pullToRefreshModifier) {
        LazyVerticalGrid(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium),
            columns = GridCells.Adaptive(300.dp),
            verticalArrangement = Arrangement.spacedBy(RuuviStationTheme.dimensions.medium),
            horizontalArrangement = Arrangement.spacedBy(RuuviStationTheme.dimensions.medium)
        ) {
            items(items, key = { it.id }) { sensor ->
                when (dashboardType) {
                    DashboardType.SIMPLE_VIEW ->
                        DashboardItemSimple(
                            sensor = sensor,
                            userEmail = userEmail,
                            dashboardTapAction = dashboardTapAction,
                            setName = setName
                        )
                    DashboardType.IMAGE_VIEW ->
                        DashboardItem(
                            itemHeight = itemHeight,
                            sensor = sensor,
                            userEmail = userEmail,
                            dashboardTapAction = dashboardTapAction,
                            setName = setName
                        )
                }
            }
            item(span = { GridItemSpan(Int.MAX_VALUE) }) { Box(modifier = Modifier.navigationBarsPadding()) }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardItem(
    itemHeight: Dp,
    sensor: RuuviTag,
    userEmail: String?,
    dashboardTapAction: DashboardTapAction,
    setName: (String, String?) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .height(itemHeight)
            .fillMaxWidth()
            .clickableSingle {
                SensorCardActivity.start(
                    context,
                    sensor.id,
                    dashboardTapAction == DashboardTapAction.SHOW_CHART
                )
            },
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = RuuviStationTheme.colors.dashboardCardBackground
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = 0.25f)
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
                val (title, bigTemperature, buttons, values, updated) = createRefs()

                ItemName(
                    sensor = sensor,
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(buttons.start)
                        width = Dimension.fillToConstraints
                    }
                )

                ItemButtons(
                    sensor = sensor,
                    userEmail = userEmail,
                    modifier = Modifier
                        .constrainAs(buttons) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        },
                    setName = setName
                )

                if (sensor.latestMeasurement?.temperatureValue != null) {
                    BigValueDisplay(
                        value = sensor.latestMeasurement.temperatureValue,
                        alertTriggered = sensor.alarmSensorStatus.triggered(AlarmType.TEMPERATURE),
                        modifier = Modifier
                            .constrainAs(bigTemperature) {
                                top.linkTo(title.bottom)
                                start.linkTo(parent.start)
                            }
                    )
                }

                ItemValuesWithoutTemperature(
                    sensor = sensor,
                    modifier = Modifier.constrainAs(values) {
                        start.linkTo(parent.start)
                        bottom.linkTo(updated.top)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                )

                ItemBottom(
                    sensor = sensor,
                    modifier = Modifier
                        .constrainAs(updated) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardItemSimple(
    sensor: RuuviTag,
    userEmail: String?,
    dashboardTapAction: DashboardTapAction,
    setName: (String, String?) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableSingle {
                SensorCardActivity.start(
                    context,
                    sensor.id,
                    dashboardTapAction == DashboardTapAction.SHOW_CHART
                )
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
            val (title, buttons, updated, values) = createRefs()

            ItemName(
                sensor = sensor,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(buttons.start)
                    width = Dimension.fillToConstraints
                }
            )

            ItemButtons(
                sensor = sensor,
                userEmail = userEmail,
                modifier = Modifier
                    .constrainAs(buttons) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                setName = setName
            )

            ItemValues(
                sensor = sensor,
                modifier = Modifier
                    .padding(vertical = RuuviStationTheme.dimensions.small)
                    .constrainAs(values) {
                        top.linkTo(title.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
            )

            ItemBottom(
                sensor = sensor,
                modifier = Modifier.constrainAs(updated) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(values.bottom)
                    width = Dimension.fillToConstraints
                }
            )
        }
    }
}

@Composable
fun ItemName(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    Text(
        style = RuuviStationTheme.typography.title,
        text = sensor.displayName,
        lineHeight = RuuviStationTheme.fontSizes.extended,
        fontSize = RuuviStationTheme.fontSizes.normal,
        modifier = modifier,
        maxLines = 2
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemButtons(
    sensor: RuuviTag,
    userEmail: String?,
    setName: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .width(RuuviStationTheme.dimensions.dashboardIconSize * 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
            if (sensor.alarmSensorStatus is AlarmSensorStatus.NotTriggered) {
                IconButton(
                    modifier = Modifier.size(RuuviStationTheme.dimensions.dashboardIconSize),
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
            } else if (sensor.alarmSensorStatus is AlarmSensorStatus.Triggered) {
                BlinkingEffect() {
                    IconButton(
                        modifier = Modifier.size(RuuviStationTheme.dimensions.dashboardIconSize),
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
                Spacer(modifier = Modifier.size(RuuviStationTheme.dimensions.dashboardIconSize))
            }

            DashboardItemDropdownMenu(sensor, userEmail, setName)
        }
    }
}

@Composable
fun ItemValues(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Top,
        modifier = modifier
    ) {
        if (sensor.latestMeasurement != null) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
            ) {
                ValueDisplay(
                    value = sensor.latestMeasurement.temperatureValue,
                    sensor.alarmSensorStatus.triggered(AlarmType.TEMPERATURE)
                )
                if (sensor.latestMeasurement.humidityValue != null) {
                    ValueDisplay(
                        value = sensor.latestMeasurement.humidityValue,
                        sensor.alarmSensorStatus.triggered(AlarmType.HUMIDITY)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (sensor.latestMeasurement.pressureValue != null) {
                    ValueDisplay(
                        value = sensor.latestMeasurement.pressureValue,
                        sensor.alarmSensorStatus.triggered(AlarmType.PRESSURE)
                    )
                }

                if (sensor.latestMeasurement.movementValue != null) {
                    ValueDisplay(
                        value = sensor.latestMeasurement.movementValue,
                        sensor.alarmSensorStatus.triggered(AlarmType.MOVEMENT)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemValuesWithoutTemperature(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        Row(
            verticalAlignment = Top,
            modifier = modifier
        ) {

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                if (sensor.latestMeasurement.humidityValue != null) {
                    ValueDisplay(
                        value = sensor.latestMeasurement.humidityValue,
                        sensor.alarmSensorStatus.triggered(AlarmType.HUMIDITY)
                    )
                }
                if (sensor.latestMeasurement.movementValue != null) {
                    ValueDisplay(
                        value = sensor.latestMeasurement.movementValue,
                        sensor.alarmSensorStatus.triggered(AlarmType.MOVEMENT)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                if (sensor.latestMeasurement.pressureValue != null) {
                    ValueDisplay(
                        value = sensor.latestMeasurement.pressureValue,
                        sensor.alarmSensorStatus.triggered(AlarmType.PRESSURE)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemBottom(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        ItemBottomUpdatedInfo(sensor = sensor, modifier = modifier)
    } else {
        ItemBottomNoData(modifier = modifier)
    }
}

@Composable
fun ItemBottomNoData(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.padding(top = 2.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = RuuviStationTheme.typography.dashboardSecondary,
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.no_data_10_days),
        )
    }
}

@Composable
fun ItemBottomUpdatedInfo(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {

        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current

        var updatedText by remember {
            mutableStateOf(sensor.latestMeasurement.updatedAt?.describingTimeSince(context) ?: "")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(top = 2.dp)
        ) {
            val icon = if (sensor.latestMeasurement.updatedAt == sensor.networkLastSync) {
                R.drawable.ic_icon_gateway
            } else {
                R.drawable.ic_icon_bluetooth
            }
            Icon(
                modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus),
                painter = painterResource(id = icon),
                tint = RuuviStationTheme.colors.primary.copy(alpha = 0.5f),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.small))

            Text(
                modifier = Modifier.weight(1f),
                style = RuuviStationTheme.typography.dashboardSecondary,
                text = updatedText,
            )

            if (sensor.isLowBattery()) {
                LowBattery(modifier = Modifier.weight(1f))
            }
        }

        LaunchedEffect(key1 = lifecycle, key2 = sensor.latestMeasurement.updatedAt) {
            lifecycle.whenStarted {
                while (true) {
                    updatedText = sensor.latestMeasurement.updatedAt?.describingTimeSince(context) ?: ""
                    delay(500)
                }
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
    GlideImage(
        modifier = Modifier.fillMaxSize(),
        model = rememberResourceUri(R.drawable.tag_bg_layer),
        contentDescription = null,
        alpha = RuuviStationTheme.colors.backgroundAlpha,
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
            modifier = Modifier.alignByBaseline(),
            text = value.valueWithoutUnit,
            style = RuuviStationTheme.typography.dashboardValue,
            color = textColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(width = 4.dp))
        Text(
            modifier = Modifier.alignByBaseline(),
            text = value.unitString,
            style = RuuviStationTheme.typography.dashboardUnit,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun BigValueDisplay(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlert
    } else {
        RuuviStationTheme.colors.settingsTitleText
    }

    Row(
        modifier = modifier
            .offset(y = (-8).dp * LocalDensity.current.fontScale),
        verticalAlignment = Top
    ) {
        Text(
            style = RuuviStationTheme.typography.dashboardBigValue,
            text = value.valueWithoutUnit,
            lineHeight = 10.sp,
            color = textColor
        )
        Text(
            modifier = Modifier
                .padding(
                    top = 8.dp * LocalDensity.current.fontScale,
                    start = 2.dp
                ),
            style = RuuviStationTheme.typography.dashboardBigValueUnit,
            text = value.unitString,
            color = textColor
        )
    }
}

@Composable
fun DashboardItemDropdownMenu(
    sensor: RuuviTag,
    userEmail: String?,
    setName: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var threeDotsMenuExpanded by remember {
        mutableStateOf(false)
    }

    var setNameDialog by remember { mutableStateOf(false) }

    val canBeShared = sensor.owner == userEmail
    val canBeClaimed = sensor.owner.isNullOrEmpty() && userEmail?.isNotEmpty() == true

    Box() {
        IconButton(
            modifier = Modifier.size(RuuviStationTheme.dimensions.dashboardIconSize),
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
                SensorCardActivity.start(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.full_image_view
                ))
            }

            DropdownMenuItem(onClick = {
                SensorCardActivity.start(context, sensor.id, true)
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
                BackgroundActivity.start(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.change_background
                ))
            }

            DropdownMenuItem(onClick = {
                setNameDialog = true
                threeDotsMenuExpanded = false
            }) {
                com.ruuvi.station.app.ui.components.Paragraph(text = stringResource(
                    id = R.string.rename
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

    if (setNameDialog) {
        SetSensorName(
            value = sensor.name,
            setName = {newName ->
                setName.invoke(sensor.id, newName)
            }
        ) {
            setNameDialog = false
        }
    }
}

@Composable
fun LowBattery(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        Text(
            style = RuuviStationTheme.typography.dashboardSecondary,
            text = stringResource(id = R.string.low_battery),
        )
        Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
        Image(
            modifier = Modifier
                .height(12.dp)
                .align(CenterVertically),
            painter = painterResource(id = R.drawable.icon_battery_low),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.mediumPlus))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
}