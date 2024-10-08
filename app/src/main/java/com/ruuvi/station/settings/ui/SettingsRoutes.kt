package com.ruuvi.station.settings.ui

import android.content.Context
import com.ruuvi.station.R

object SettingsRoutes {
    const val LIST = "list"
    const val TEMPERATURE = "temperature"
    const val HUMIDITY = "humidity"
    const val PRESSURE = "pressure"
    const val APPEARANCE = "appearance"
    const val ALERT_NOTIFICATIONS = "alert_notifications"
    const val BACKGROUNDSCAN = "backgroundscan"
    const val CHARTS = "charts"
    const val CLOUD = "cloud"
    const val DATAFORWARDING = "dataforwarding"
    const val DEVELOPER = "developer"
    const val SHARINGWEB = "sharingweb"

    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            APPEARANCE -> context.getString(R.string.settings_appearance)
            LIST -> context.getString(R.string.menu_app_settings)
            TEMPERATURE -> context.getString(R.string.settings_temperature)
            HUMIDITY -> context.getString(R.string.settings_humidity)
            PRESSURE -> context.getString(R.string.settings_pressure)
            BACKGROUNDSCAN -> context.getString(R.string.settings_background_scan)
            CHARTS -> context.getString(R.string.settings_chart)
            CLOUD -> context.getString(R.string.ruuvi_cloud)
            DATAFORWARDING -> context.getString(R.string.settings_data_forwarding)
            ALERT_NOTIFICATIONS -> context.getString(R.string.settings_alert_notifications)
            DEVELOPER -> context.getString(R.string.settings_developer)
            else -> context.getString(R.string.menu_app_settings)
        }
    }
}