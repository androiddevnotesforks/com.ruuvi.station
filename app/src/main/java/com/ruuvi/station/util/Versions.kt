package com.ruuvi.station.util

import net.swiftzer.semver.SemVer

private fun String.toSemVerOrNull(): SemVer? {
    val firstDigitIndex = indexOfFirst { it.isDigit() }
    if (firstDigitIndex == -1) return null

    val versionPart = substring(firstDigitIndex)
    return runCatching { SemVer.parse(versionPart) }.getOrNull()
}

fun String.compareVersionTo(other: String): Int? {
    val v1 = this.toSemVerOrNull()
    val v2 = other.toSemVerOrNull()
    if (v1 == null || v2 == null) return null

    val majorCmp = v1.major.compareTo(v2.major)
    if (majorCmp != 0) return majorCmp

    val minorCmp = v1.minor.compareTo(v2.minor)
    if (minorCmp != 0) return minorCmp

    val patchCmp = v1.patch.compareTo(v2.patch)
    if (patchCmp != 0) return patchCmp

    val v1HasDev = this.contains("dev", ignoreCase = true)
    val v2HasDev = other.contains("dev", ignoreCase = true)

    return when {
        v1HasDev && !v2HasDev -> -1
        !v1HasDev && v2HasDev -> 1
        else -> 0
    }
}

fun isNewerVersion(current: String, latest: String): Boolean {
    val cmp = current.compareVersionTo(latest) ?: return false
    return cmp < 0
}

fun isEqualVersion(current: String, latest: String): Boolean {
    val cmp = current.compareVersionTo(latest) ?: return false
    return cmp == 0
}