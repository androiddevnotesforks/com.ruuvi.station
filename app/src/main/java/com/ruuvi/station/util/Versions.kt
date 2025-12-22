package com.ruuvi.station.util

import net.swiftzer.semver.SemVer

private fun String.toSemVerOrNull(): SemVer? {
    val firstDigitIndex = indexOfFirst { it.isDigit() }
    if (firstDigitIndex == -1) return null

    val versionPart = substring(firstDigitIndex)
    return runCatching { SemVer.parse(versionPart) }.getOrNull()
}

/**
 * @return positive if [this] > [other],
 *         0 if equal,
 *         negative if [this] < [other].
 * Returns null if any of them can't be parsed.
 */
fun String.compareVersionTo(other: String): Int? {
    val v1 = this.toSemVerOrNull()
    val v2 = other.toSemVerOrNull()
    if (v1 == null || v2 == null) return null
    return v1.compareTo(v2)
}

fun isNewerVersion(current: String, latest: String): Boolean {
    val cmp = current.compareVersionTo(latest) ?: return false
    return cmp < 0
}

fun isEqualVersion(current: String, latest: String): Boolean {
    val cmp = current.compareVersionTo(latest) ?: return false
    return cmp == 0
}