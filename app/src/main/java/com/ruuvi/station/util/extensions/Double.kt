package com.ruuvi.station.util.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun Double.equalsEpsilon(other: Double, epsilon: Double = 0.00000001) = abs(this - other) < epsilon

fun Double.diff(second: Double): Double = Math.abs(this - second)

fun Double.round(decimals: Int): Double {
    if (!this.isFinite()) {
        return this
    }
    return BigDecimal.valueOf(this)
        .setScale(decimals, RoundingMode.HALF_UP)
        .toDouble()
}