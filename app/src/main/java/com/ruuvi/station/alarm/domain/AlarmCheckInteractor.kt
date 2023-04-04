package com.ruuvi.station.alarm.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.util.extensions.diff
import timber.log.Timber
import java.util.*

class AlarmCheckInteractor(
    private val context: Context,
    private val tagConverter: TagConverter,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val alarmRepository: AlarmRepository,
    private val unitsConverter: UnitsConverter,
    private val alertNotificationInteractor: AlertNotificationInteractor
) {
    private val lastFiredNotification = mutableMapOf<Int, Long>()

    fun getStatus(ruuviTag: RuuviTag): AlarmStatus {
        val alarms = getEnabledAlarms(ruuviTag)
        if (alarms.isEmpty()) return AlarmStatus.NO_ALARM
        alarms
            .forEach { alarm ->
                if (AlarmChecker(ruuviTag, alarm).triggered) {
                    return AlarmStatus.TRIGGERED
                }
            }
        return AlarmStatus.NO_TRIGGERED
    }

    fun getAlarmStatus(ruuviTag: RuuviTag): AlarmSensorStatus {
        val alarms = getEnabledAlarms(ruuviTag)
        if (alarms.isEmpty()) return AlarmSensorStatus.NoAlarms
        val triggeredTypes = mutableSetOf<AlarmType>()
        alarms
            .forEach { alarm ->
                if (AlarmChecker(ruuviTag, alarm).triggered) {
                    triggeredTypes.add(alarm.alarmType)
                }
            }
        if (triggeredTypes.isEmpty()) {
            return AlarmSensorStatus.NotTriggered
        } else {
            return AlarmSensorStatus.Triggered(triggeredTypes)
        }
    }

    fun checkAlarm(sensor: RuuviTag, alarm: Alarm): Boolean {
        return AlarmChecker(sensor, alarm).triggered
    }

    fun checkAlarmsForSensor(sensor: RuuviTagEntity, sensorSettings: SensorSettings) {
        val ruuviTag = tagConverter.fromDatabase(sensor, sensorSettings)
        getEnabledAlarms(ruuviTag)
            .forEach { alarm ->
                val checker = AlarmChecker(ruuviTag, alarm)
                if (checker.triggered && canNotify(alarm)) {
                    sendAlert(checker)
                }
            }
    }

    private fun getEnabledAlarms(ruuviTag: RuuviTag): List<Alarm> =
        alarmRepository.getForSensor(ruuviTag.id).filter { it.enabled }

    private fun canNotify(alarm: Alarm): Boolean {
        val lastNotificationTime = lastFiredNotification[alarm.id]
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.add(Calendar.SECOND, -NOTIFICATION_THRESHOLD_SECONDS)
        val notificationThreshold = calendar.timeInMillis
        val alarmMutedTill = alarm.mutedTill
        val muted = alarmMutedTill != null && alarmMutedTill.time > now
        return if (!muted && (lastNotificationTime == null || lastNotificationTime < notificationThreshold)) {
            lastFiredNotification[alarm.id] = now
            true
        } else {
            false
        }
    }

    private fun sendAlert(checker: AlarmChecker) {
        Timber.d("sendAlert tag.tagName = ${checker.ruuviTag}tagName; alarm.id = ${checker.alarm.id}; notificationResourceId = ${checker.alarmResource}")
        val message = checker.getMessage()
        if (message?.isNotEmpty() == true) {
            val notificationData = AlertNotificationInteractor.AlertNotificationData(
                sensorId = checker.ruuviTag.id,
                message = message,
                alarmId = checker.alarm.id,
                sensorName = checker.ruuviTag.displayName,
                alertCustomDescription = checker.alarm.customDescription
            )

            alertNotificationInteractor.notify(checker.alarm.id, notificationData)
        }
    }

    fun removeNotificationById(notificationId: Int) {
        alertNotificationInteractor.removeNotificationById(notificationId)
    }

    inner class AlarmChecker(
        val ruuviTag: RuuviTag,
        val alarm: Alarm
    ) {
        var alarmResource: Int? = null
        private var thresholdValue: Double = 0.0

        val triggered: Boolean
            get() = alarmResource != null

        init {
            checkAlarmStatus()
        }

        fun getMessage(): String? {
            alarmResource?.let { resource ->
                return when (alarm.alarmType) {
                    AlarmType.HUMIDITY -> {
                        val displayThreshold = unitsConverter.getDisplayValue(thresholdValue.toFloat())
                        context.getString(resource, "$displayThreshold ${unitsConverter.getHumidityUnitString(HumidityUnit.PERCENT)}")
                    }
                    AlarmType.PRESSURE -> {
                        val displayThreshold = unitsConverter.getDisplayValue(unitsConverter.getPressureValue(thresholdValue).toFloat())
                        val thresholdString = "$displayThreshold ${unitsConverter.getPressureUnitString()}"
                        context.getString(resource, thresholdString)
                    }
                    AlarmType.TEMPERATURE -> {
                        val displayThreshold = unitsConverter.getDisplayValue(unitsConverter.getTemperatureValue(thresholdValue).toFloat())
                        val thresholdString = "$displayThreshold${unitsConverter.getTemperatureUnitString()}"
                        context.getString(resource, thresholdString)
                    }
                    AlarmType.RSSI -> {
                        val displayThreshold = unitsConverter.getDisplayValue(thresholdValue.toFloat())
                        context.getString(resource, "$displayThreshold ${unitsConverter.getSignalUnit()}")
                    }
                    AlarmType.MOVEMENT -> context.getString(resource)
                    else -> null
                }
            }
            return null
        }

        private fun checkAlarmStatus() {
            when (alarm.alarmType) {
                AlarmType.HUMIDITY,
                AlarmType.PRESSURE,
                AlarmType.TEMPERATURE,
                AlarmType.RSSI -> compareWithAlarmRange()
                AlarmType.MOVEMENT -> checkMovementData()
            }
        }

        private fun compareWithAlarmRange() {
            when (alarm.type) {
                Alarm.TEMPERATURE ->
                    if (ruuviTag.temperature != null) {
                        compareValues(
                            ruuviTag.temperature,
                            R.string.alert_notification_temperature_low_threshold to
                                R.string.alert_notification_temperature_high_threshold
                        )
                    }
                Alarm.HUMIDITY ->
                    if (ruuviTag.humidity != null) {
                        compareValues(
                            ruuviTag.humidity,
                            R.string.alert_notification_humidity_low_threshold to
                                R.string.alert_notification_humidity_high_threshold
                        )
                    }
                Alarm.PRESSURE ->
                    if (ruuviTag.pressure != null) {
                        compareValues(
                            ruuviTag.pressure,
                            R.string.alert_notification_pressure_low_threshold to
                                R.string.alert_notification_pressure_high_threshold
                        )
                    }
                Alarm.RSSI ->
                    compareValues(
                        ruuviTag.rssi,
                        R.string.alert_notification_rssi_low_threshold to
                            R.string.alert_notification_rssi_high_threshold
                    )
            }
        }

        private fun checkMovementData() {
            val readings: List<TagSensorReading> =
                sensorHistoryRepository.getLatestForSensor(ruuviTag.id, 2)
            if (readings.size == 2) {
                alarmResource = when {
                    ruuviTag.dataFormat == FORMAT5 && readings.first().movementCounter != readings.last().movementCounter -> R.string.alert_notification_movement
                    ruuviTag.dataFormat != FORMAT5 && hasTagMoved(readings.first(), readings.last()) -> R.string.alert_notification_movement
                    else -> null
                }
            }
        }

        private fun compareValues(
            comparedValue: Number,
            resources: Pair<Int, Int>
        ) {
            val (lowResourceId, highResourceId) = resources
            when {
                comparedValue.toDouble() < alarm.min -> {
                    alarmResource = lowResourceId
                    thresholdValue = alarm.min
                }
                comparedValue.toDouble() > alarm.max -> {
                    alarmResource = highResourceId
                    thresholdValue = alarm.max
                }
            }
        }

        private fun hasTagMoved(one: TagSensorReading, two: TagSensorReading): Boolean {
            val accelX1 = one.accelX ?: 0.0
            val accelY1 = one.accelY ?: 0.0
            val accelZ1 = one.accelZ ?: 0.0
            val accelX2 = two.accelX ?: 0.0
            val accelY2 = two.accelY ?: 0.0
            val accelZ2 = two.accelZ ?: 0.0
            return accelZ1.diff(accelZ2) > MOVEMENT_THRESHOLD ||
                    accelY1.diff(accelY2) > MOVEMENT_THRESHOLD ||
                    accelX1.diff(accelX2) > MOVEMENT_THRESHOLD
        }
    }

    companion object {
        private const val FORMAT5 = 5
        private const val MOVEMENT_THRESHOLD = 0.03
        private const val NOTIFICATION_THRESHOLD_SECONDS = 30
    }
}

enum class AlarmStatus {
    TRIGGERED,
    NO_TRIGGERED,
    NO_ALARM
}

sealed class AlarmSensorStatus {
    object NoAlarms: AlarmSensorStatus()
    object NotTriggered: AlarmSensorStatus()
    class Triggered(val alarmTypes: Set<AlarmType>): AlarmSensorStatus()

    fun triggered(alarmType: AlarmType): Boolean {
        return if (this is Triggered) {
            alarmTypes.contains(alarmType)
        } else {
            false
        }
    }
}