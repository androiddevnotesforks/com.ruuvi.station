package com.ruuvi.station.dfu.data

data class FirmwareInfo(
    val version: String,
    val url: String,
    val created_at: String,
    val versionCode: Int,
    val fileName: String,
    val fwloader: String,
    val mcuboot_s1: String,
    val mcuboot: String
)

data class FirmwareData(
    val latest: FirmwareInfo?,
    val beta: FirmwareInfo?,
    val alpha: FirmwareInfo?,
)

data class FirmwareResponse(
    val result: String,
    val data: FirmwareData
)