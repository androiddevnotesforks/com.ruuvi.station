package com.ruuvi.station.network.data.request

import com.ruuvi.station.database.tables.Alarm
import java.util.*

data class SetAlertRequest(
    val sensor: String,
    val type: String,
    val min: Double,
    val max: Double,
    val enabled: Boolean,
    val description: String,
    val timestamp: Long = Date().time / 1000
) {
    companion object {
        fun getAlarmRequest(alarm: Alarm): SetAlertRequest {
            return SetAlertRequest(
                sensor = alarm.ruuviTagId,
                type = alarm.alarmType?.networkCode ?: throw IllegalArgumentException(),
                min = alarm.min,
                max = alarm.max,
                enabled = alarm.enabled,
                description = alarm.customDescription
            )
        }
    }
}