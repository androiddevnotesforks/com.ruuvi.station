package com.ruuvi.station.app.di

import com.ruuvi.station.about.di.AboutActivityInjectionModule
import com.ruuvi.station.addtag.di.AddTagActivityInjectionModule
import com.ruuvi.station.alarm.di.AlarmModule
import com.ruuvi.station.bluetooth.di.BluetoothScannerInjectionModule
import com.ruuvi.station.dashboard.di.DashboardActivityInjectionModule
import com.ruuvi.station.gateway.di.GatewayInjectionModule
import com.ruuvi.station.graph.di.GraphInjectionModule
import com.ruuvi.station.settings.di.SettingsInjectionModule
import com.ruuvi.station.startup.di.StartupActivityInjectionModule
import com.ruuvi.station.tag.di.RuuviTagInjectionModule
import com.ruuvi.station.tagdetails.di.TagDetailsInjectionModule
import com.ruuvi.station.tagsettings.di.TagSettingsInjectionModule
import com.ruuvi.station.units.di.UnitsInjectionModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein

@ExperimentalCoroutinesApi
object AppInjectionModules {
    val module = Kodein.Module(AppInjectionModules.javaClass.name) {
        import(AppInjectionModule.module)
        import(PreferencesInjectionModule.module)
        import(BluetoothScannerInjectionModule.module)
        import(SettingsInjectionModule.module)
        import(GatewayInjectionModule.module)
        import(TagDetailsInjectionModule.module)
        import(DashboardActivityInjectionModule.module)
        import(StartupActivityInjectionModule.module)
        import(AboutActivityInjectionModule.module)
        import(AddTagActivityInjectionModule.module)
        import(TagSettingsInjectionModule.module)
        import(RuuviTagInjectionModule.module)
        import(AlarmModule.module)
        import(UnitsInjectionModule.module)
        import(GraphInjectionModule.module)
    }
}