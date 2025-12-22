package com.ruuvi.station.dfu.data

import java.io.File

sealed class DownloadFileStatus{
    data class Progress(val title: String, val percent: Int): DownloadFileStatus()
    object Finished: DownloadFileStatus()
    data class Failed(val error: String): DownloadFileStatus()
}
