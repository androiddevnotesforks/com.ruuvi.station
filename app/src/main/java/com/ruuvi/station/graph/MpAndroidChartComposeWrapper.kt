package com.ruuvi.station.graph

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.text.format.DateUtils
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.size
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.Utils
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.scaledToMax
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.isStartOfTheDay
import timber.log.Timber
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChartViewPrototype(
    lineChart: LineChart,
    modifier: Modifier,
    chartData: MutableList<Entry>,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    limits: Pair<Double,Double>?,
    from: Long,
    to: Long,
    clearMarker: () -> Unit
) {
    val context = LocalContext.current
    Timber.d("ChartView - ChartViewPrototype")

    val title = when (chartSensorType) {
        ChartSensorType.TEMPERATURE -> stringResource(id = R.string.temperature_with_unit, unitsConverter.getTemperatureUnitString())
        ChartSensorType.HUMIDITY -> stringResource(id = R.string.humidity_with_unit, unitsConverter.getHumidityUnitString())
        ChartSensorType.PRESSURE -> stringResource(id = R.string.pressure_with_unit, unitsConverter.getPressureUnitString())
        ChartSensorType.BATTERY -> stringResource(id = R.string.battery_voltage)
        ChartSensorType.ACCELERATION -> stringResource(id = R.string.acceleration_x)
        ChartSensorType.RSSI -> stringResource(id = R.string.signal_strength_rssi)
        ChartSensorType.MOVEMENTS -> stringResource(id = R.string.movement_counter)
        ChartSensorType.CO2 -> stringResource(id = R.string.co2_with_unit, stringResource(id = R.string.unit_co2))
        ChartSensorType.VOC -> stringResource(id = R.string.voc_with_unit, stringResource(id = R.string.unit_voc))
        ChartSensorType.NOX -> stringResource(id = R.string.nox_with_unit, stringResource(id = R.string.unit_nox))
        ChartSensorType.PM25 -> stringResource(id = R.string.pm25_with_unit, stringResource(id = R.string.unit_pm25))
        ChartSensorType.LUMINOSITY -> stringResource(id = R.string.luminosity_with_unit, stringResource(id = R.string.unit_luminosity))
        ChartSensorType.SOUND -> stringResource(id = R.string.sound_with_unit, stringResource(id = R.string.unit_sound))
        ChartSensorType.AQI -> stringResource(id = R.string.aqi)
    }

    val offset = RuuviStationTheme.dimensions.extended
    val description = getPrototypeChartDescription(
        context,
        lineChart,
        chartData,
        unitsConverter,
        chartSensorType
    )

    Column (
        modifier = modifier
    ){
        Text(
            modifier = Modifier.padding(start = offset, top = RuuviStationTheme.dimensions.medium),
            style = RuuviStationTheme.typography.subtitle,
            fontSize = RuuviStationTheme.fontSizes.miniature.scaledToMax(max = 20.sp),
            text = title,
            color = RuuviStationTheme.colors.buttonText
        )
        if (showChartStats) {
            Text(
                modifier = Modifier.padding(
                    start = offset,
                    bottom = RuuviStationTheme.dimensions.small,
                    end = RuuviStationTheme.dimensions.medium
                ),
                style = RuuviStationTheme.typography.paragraphSmall,
                text = description,
                fontSize = RuuviStationTheme.fontSizes.miniature.scaledToMax(max = 20.sp),
                color = RuuviStationTheme.colors.buttonText
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onGloballyPositioned {
                    Timber.d("setLabelCount onGloballyPositioned")
                    setLabelCount(context, lineChart)
                }
                .padding(horizontal = RuuviStationTheme.dimensions.medium),
            factory = { context ->
                Timber.d("ChartView AndroidView - factory")
                val chart = lineChart
                setupMarker(context, chart, unitsConverter, chartSensorType, clearMarker = clearMarker, getFrom =  { from })
                chart
            },
            update = { view ->
                Timber.d("ChartView AndroidView - update $from pointsCount = ${chartData.size}")
                addDataToChart(context, chartData, view, "", graphDrawDots, limits, from, to)
                (view.marker as ChartMarkerView).getFrom = {from} }
        )
    }
}

fun getPrototypeChartDescription(
    context: Context,
    lineChart: LineChart,
    chartData: MutableList<Entry>,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType,
): String {
    Timber.d("ChartView - getPrototypeChartDescription")

    fun getSimpleValue (value: Double, x: Int?): String {
        return value.toInt().toString()
    }

    val getRawValue = when (chartSensorType) {
        ChartSensorType.TEMPERATURE -> unitsConverter::getTemperatureRawWithoutUnitString
        ChartSensorType.HUMIDITY -> unitsConverter::getHumidityRawWithoutUnitString
        ChartSensorType.PRESSURE -> unitsConverter::getPressureRawWithoutUnitString
        ChartSensorType.BATTERY -> unitsConverter::getTemperatureRawWithoutUnitString
        ChartSensorType.ACCELERATION -> unitsConverter::getTemperatureRawWithoutUnitString
        ChartSensorType.RSSI -> unitsConverter::getTemperatureRawWithoutUnitString
        ChartSensorType.MOVEMENTS -> unitsConverter::getTemperatureRawWithoutUnitString
        ChartSensorType.CO2 -> ::getSimpleValue
        ChartSensorType.VOC -> ::getSimpleValue
        ChartSensorType.NOX -> ::getSimpleValue
        ChartSensorType.PM25 -> ::getSimpleValue
        ChartSensorType.LUMINOSITY -> ::getSimpleValue
        ChartSensorType.SOUND -> ::getSimpleValue
        ChartSensorType.AQI -> ::getSimpleValue
    }

    val latestPoint = chartData.lastOrNull()
    val latestValue =
        if (latestPoint != null) getRawValue(latestPoint.y.toDouble(), null) else ""

    val lowestVisibleX = lineChart.lowestVisibleX
    val highestVisibleX = lineChart.highestVisibleX
    val visibleEntries = chartData.filter { it.x >= lowestVisibleX && it.x <= highestVisibleX }
    Timber.d("calculateCaption low = $highestVisibleX high = $highestVisibleX count = ${visibleEntries.size}")

    if (visibleEntries.isEmpty())
        return context.getString(R.string.chart_latest_min_max_avg, "", "", "", latestValue)

    var totalArea = 0.0

    var min = visibleEntries[0].y
    var max = visibleEntries[0].y

    for (i in 1 until visibleEntries.size) {
        val x1 = visibleEntries[i - 1].x
        val y1 = visibleEntries[i - 1].y
        val x2 = visibleEntries[i].x
        val y2 = visibleEntries[i].y

        val area = (x2 - x1) * (y2 + y1) / 2.0
        totalArea += area

        if (y2 < min) min = y2
        if (y2 > max) max = y2
    }
    val timespan = visibleEntries.last().x - visibleEntries.first().x

    val average = if (timespan != 0f) (totalArea / timespan).toFloat() else visibleEntries.first().y

    return context.getString(
        R.string.chart_latest_min_max_avg,
        getRawValue(min.toDouble(), null),
        getRawValue(max.toDouble(), null),
        getRawValue(average.toDouble(), null),
        latestValue
    )
}

fun chartsInitialSetup(
    context: Context,
    unitsConverter: UnitsConverter,
    charts: List<Pair<ChartSensorType, LineChart>>
) {
    for (chartPair in charts) {
        setupChart(chartPair.second, unitsConverter, chartPair.first)
        applyChartStyle(
            context = context,
            chart = chartPair.second,
        )
    }

    normalizeOffsets(charts)
    synchronizeChartGestures(charts.map { it.second }.toSet())
    setupHighLighting(charts.map { it.second }.toSet())
}


fun applyChartStyle(
    context: Context,
    chart: LineChart
) {
    chart.axisRight.isEnabled = false

    chart.xAxis.textColor = Color.WHITE
    chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

    chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
    chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
    chart.axisLeft.isGranularityEnabled = true
    chart.description.textColor = Color.WHITE
    chart.description.yOffset = 5f
    chart.description.xOffset = 5f
    chart.dragDecelerationFrictionCoef = 0.8f
    chart.setNoDataTextColor(Color.WHITE)
    chart.viewPortHandler.setMaximumScaleX(5000f)
    chart.viewPortHandler.setMaximumScaleY(30f)
    chart.setTouchEnabled(true)
    chart.xAxis.gridColor = ColorUtils.setAlphaComponent(chart.xAxis.gridColor, 100)
    chart.axisLeft.gridColor = ColorUtils.setAlphaComponent(chart.xAxis.gridColor, 100)
    chart.isDoubleTapToZoomEnabled = false
    chart.isHighlightPerTapEnabled = true

    try {
        val font = ResourcesCompat.getFont(context, R.font.mulish_regular)
        chart.description.typeface = font
        chart.axisLeft.typeface = font
        chart.xAxis.typeface = font
    } catch (e: Exception) {
        Timber.e(e)
    }

    var textSize = context.resources.getDimension(R.dimen.graph_description_size)
    val density = context.resources.displayMetrics.density
    if (density < 2) textSize *= 2
    textSize = min(textSize, 20f)
    Timber.d("graph_description_sizegraph_description_size $textSize $density")
    chart.description.textSize = textSize
    chart.axisLeft.textSize = textSize
    chart.xAxis.textSize = textSize
    chart.legend.isEnabled = false
}

fun setupMarker(
    context: Context,
    chart: LineChart,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType,
    getFrom: () -> Long,
    clearMarker: () -> Unit
) {
    Timber.d("ChartView - setupMarker")
    val markerView = ChartMarkerView(
        context = context,
        layoutResource = R.layout.custom_marker_view,
        chartSensorType = chartSensorType,
        unitsConverter = unitsConverter,
        getFrom = getFrom,
        clearMarker = clearMarker
    )
    markerView.chartView = chart
    chart.marker = markerView
}

private fun addDataToChart(
    context: Context,
    data: MutableList<Entry>,
    chart: LineChart,
    label: String,
    graphDrawDots: Boolean,
    limits: Pair<Double,Double>?,
    from: Long,
    to: Long
) {
    Timber.d("ChartView - addDataToChart")
    val set = LineDataSet(data, label)
    set.setDrawCircles(graphDrawDots)
    set.setDrawValues(false)
    set.setDrawFilled(true)
    set.maximumGapBetweenPoints = 3_600_000F
    set.lineWidth = 1f
    set.circleRadius = 1.5f
    set.color = ContextCompat.getColor(context, R.color.chartLineColor)
    set.setCircleColor(ContextCompat.getColor(context, R.color.chartLineColor))
    set.setDrawCircleHole(false)
    set.fillColor = ContextCompat.getColor(context, R.color.chartFillColor)
    set.enableDashedHighlightLine(10f, 5f, 0f)
    set.setDrawHighlightIndicators(true)
    set.highLightColor = ContextCompat.getColor(context, R.color.chartLineColor)

    chart.setXAxisRenderer(
        CustomXAxisRenderer(
            from,
            chart.viewPortHandler,
            chart.xAxis,
            chart.getTransformer(YAxis.AxisDependency.LEFT)
        )
    )
    chart.rendererLeftYAxis = CustomYAxisRenderer(
        chart.viewPortHandler,
        chart.axisLeft,
        chart.getTransformer(YAxis.AxisDependency.LEFT)
    )
    chart.xAxis.axisMaximum = (to - from).toFloat()
    chart.xAxis.axisMinimum = 0f

    chart.axisLeft.removeAllLimitLines()
    if (limits != null) {
        chart.axisLeft.addLimitLine(getLimitLine(context, limits.first.toFloat()))
        chart.axisLeft.addLimitLine(getLimitLine(context, limits.second.toFloat()))
    }

    chart.description.text = label
    chart.axisLeft.axisMinimum = set.yMin - 1f
    chart.axisLeft.axisMaximum = set.yMax + 1f
    chart.axisLeft.setDrawTopYLabelEntry(false)
    chart.axisLeft.valueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(p0: Double, p1: AxisBase?): String {
            return formatDoubleToString(p0)
        }
    }

    chart.data = LineData(set)
    chart.data.isHighlightEnabled = true
    chart.xAxis.valueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Double, p1: AxisBase?): String {
            val date = Date(value.toLong() + from)
            return if (date.isStartOfTheDay()) {
                val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
                DateUtils.formatDateTime(context, date.time, flags)
            } else {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ","")
            }
        }
    }
    setLabelCount(context, chart)
}

fun getLimitLine(
    context: Context,
    value: Float
) : LimitLine {
    val limitLine = LimitLine(value)
    limitLine.lineColor = context.getColor(R.color.activeAlarm)
    limitLine.lineWidth = 1.5f
    return limitLine
}

fun formatDoubleToString(value: Double): String {
    return if (value % 1 == 0.0) {
        val symbols = DecimalFormatSymbols.getInstance()
        val decimalFormat = DecimalFormat("#,###", symbols)
        decimalFormat.format(value)
    } else {
        val symbols = DecimalFormatSymbols.getInstance()
        val decimalFormat = DecimalFormat("#.###", symbols)
        decimalFormat.format(value)
    }
}

private fun setLabelCount(context: Context, chart: LineChart) {
    val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
    val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
    val dateText = DateUtils.formatDateTime(context, Date().time, flags)

    val computePaint = Paint(1)
    computePaint.typeface = chart.xAxis.typeface
    computePaint.textSize = chart.xAxis.textSize
    val computeSize = Utils.calcTextSize(computePaint, timeText)
    val computeSize2 = Utils.calcTextSize(computePaint, dateText)
    val width = max(computeSize.width, computeSize2.width)

    val labelCount = chart.viewPortHandler.contentWidth() / (width * 2)
    val labelCountY = chart.viewPortHandler.contentHeight() / (computeSize.height * 3.5)
    Timber.d("setLabelCount ${chart.size} VIEWPORT ${chart.viewPortHandler.contentWidth()} x ${chart.viewPortHandler.contentHeight()} x = $labelCount y = $labelCountY")
    chart.xAxis.setLabelCount(labelCount.toInt(), false)
    chart.axisLeft.setLabelCount(labelCountY.toInt(), false)
    chart.notifyDataSetChanged()
    chart.invalidate()
}

// Manually setting offsets to be sure that all of the charts have equal offsets. This is needed for synchronous zoom and dragging.
fun normalizeOffsets(charts: List<Pair<ChartSensorType, LineChart>>) {
    val computePaint = Paint(1)
    computePaint.typeface = charts.first().second.axisLeft.typeface
    computePaint.textSize = charts.first().second.axisLeft.textSize
    val computeSize = Utils.calcTextSize(computePaint, "0,000.00")
    val computeHeight = Utils.calcTextHeight(computePaint, "Q").toFloat()

    val offsetLeft = computeSize.width * 1.1f
    val offsetBottom = computeHeight * 2
    val offsetTop = offsetBottom / 2f
    val offsetRight = offsetBottom / 2f

    Timber.d("Offsets top = $offsetTop bottom = $offsetBottom left = $offsetLeft right = $offsetRight computeSize = $computeSize computeHeight = $computeHeight")

    for (chart in charts) {
        chart.second.setViewPortOffsets(
            offsetLeft,
            offsetTop,
            offsetRight,
            offsetBottom
        )
    }
}

fun synchronizeChartGestures(charts: Set<LineChart>) {
    fun synchronizeCharts(sourceChart: LineChart) {
        val sourceMatrixValues = FloatArray(9)
        sourceChart.viewPortHandler.matrixTouch.getValues(sourceMatrixValues)

        charts.forEach { targetChart: LineChart ->
            if (targetChart != sourceChart) {
                val targetMatrix = targetChart.viewPortHandler.matrixTouch
                val targetMatrixValues = FloatArray(9)
                targetMatrix.getValues(targetMatrixValues)
                targetMatrixValues[Matrix.MSCALE_X] = sourceMatrixValues[Matrix.MSCALE_X]
                targetMatrixValues[Matrix.MTRANS_X] = sourceMatrixValues[Matrix.MTRANS_X]
                targetMatrixValues[Matrix.MSKEW_X] = sourceMatrixValues[Matrix.MSKEW_X]
                targetMatrix.setValues(targetMatrixValues)
                targetChart.viewPortHandler.refresh(targetMatrix, targetChart, true)
            }
        }
    }

    charts.forEach { chart: LineChart ->
        chart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureEnd(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
                charts.forEach {
                    it.setTouchEnabled(true)
                }
                synchronizeCharts(chart)
            }

            override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
            override fun onChartSingleTapped(me: MotionEvent?) {}
            override fun onChartGestureStart(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
                charts.minus(chart).forEach {
                    it.setTouchEnabled(false)
                }
            }

            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                synchronizeCharts(chart)
            }

            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartDoubleTapped(me: MotionEvent?) {}
            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                synchronizeCharts(chart)
            }
        }
    }
}

fun setupHighLighting(charts: Set<LineChart>) {
    for (chart in charts) {
        val otherCharts = charts.filter { it != chart }
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(entry: Entry, highlight: Highlight) {
                for (otherChart in otherCharts) {
                    if (!otherChart.isEmpty) {
                        otherChart.highlightValue(entry.x, highlight.dataSetIndex, false)
                    }
                }
            }

            override fun onNothingSelected() {
                for (otherChart in otherCharts) {
                    if (!otherChart.isEmpty) {
                        otherChart.highlightValue(0f, -1, false)
                    }
                }
            }
        })
    }
}