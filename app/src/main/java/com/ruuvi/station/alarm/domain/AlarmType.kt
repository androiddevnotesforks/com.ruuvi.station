package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int, val networkCode: String?, val possibleRange: IntRange, val extraRange: IntRange) {
    TEMPERATURE(0, "temperature", -40 .. 85, -55..150),
    HUMIDITY(1, "humidity", 0 .. 100, 0 .. 100),
    PRESSURE(2, "pressure", 50000 .. 115500, 50000 .. 115500),
    RSSI(3, "signal", -105 .. 0, -105 .. 0),
    MOVEMENT(4, "movement", 0 .. 0, 0 .. 0),
    OFFLINE(5, "offline", 120..86400, 120..86400);

    fun valueInRange(value: Double): Boolean = value >= extraRange.first && value <= extraRange.last

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }

        fun getByDbCode(code: Int): AlarmType = values().firstOrNull { it.value == code } ?: TEMPERATURE
    }
}