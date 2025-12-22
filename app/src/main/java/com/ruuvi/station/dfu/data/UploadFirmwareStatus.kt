package com.ruuvi.station.dfu.data

sealed class UploadFirmwareStatus{
    data class Progress(
        val percent: Int,
        val part: Int,
        val partsCount: Int
    ): UploadFirmwareStatus()
    data object Finished: UploadFirmwareStatus()
    data class Failed(val error: String): UploadFirmwareStatus()
}