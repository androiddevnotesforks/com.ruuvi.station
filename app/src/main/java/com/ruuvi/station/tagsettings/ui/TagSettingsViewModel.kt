package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.kotlinextensions.update
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class TagSettingsViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val alarmRepository: AlarmRepository
) : ViewModel() {
    var alarmElements: MutableList<AlarmElement> = ArrayList()
    var file: Uri? = null

    private var networkStatus = MutableLiveData<SensorDataResponse?>(networkInteractor.getSensorNetworkStatus(sensorId))

    private val tagState = MutableLiveData<RuuviTagEntity?>(getTagById(sensorId))
    val tagObserve: LiveData<RuuviTagEntity?> = tagState

    private val sensorSettings = MutableLiveData<SensorSettings?>()
    val sensorSettingsObserve: LiveData<SensorSettings?> = sensorSettings

    private val userLoggedIn = MutableLiveData<Boolean> (networkInteractor.signedIn)
    val userLoggedInObserve: LiveData<Boolean> = userLoggedIn

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    val sensorOwnedByUserObserve: LiveData<Boolean> = Transformations.map(sensorSettings) {
        it?.owner == networkInteractor.getEmail()
    }

    fun getTagInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            val tagInfo = getTagById(sensorId)
            val settings = interactor.getSensorSettings(sensorId)
            withContext(Dispatchers.Main) {
                tagState.value = tagInfo
                sensorSettings.value = settings
            }
        }
    }

    private val handler = CoroutineExceptionHandler() { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = exception.message
            Timber.d("CoroutineExceptionHandler: ${exception.message}")
        }
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun updateNetworkStatus() {
        networkStatus.value = networkInteractor.getSensorNetworkStatus(sensorId)
    }

    fun deleteTag(tag: RuuviTagEntity) {
        interactor.deleteTagsAndRelatives(tag)
    }

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun updateTagBackground(userBackground: String?, defaultBackground: Int?) {
        interactor.updateTagBackground(sensorId, userBackground, defaultBackground)
        if (networkInteractor.signedIn) {
            if (userBackground.isNullOrEmpty() == false) {
                networkInteractor.uploadImage(sensorId, userBackground)
            } else if (networkStatus.value?.picture.isNullOrEmpty() == false) {
                networkInteractor.resetImage(sensorId)
            }
        }
    }

    fun saveOrUpdateAlarmItems() {
        for (alarmItem in alarmElements) {
            if (alarmItem.isEnabled || alarmItem.low != alarmItem.min || alarmItem.high != alarmItem.max) {
                if (alarmItem.alarm == null) {
                    alarmItem.alarm = Alarm(
                        ruuviTagId = sensorId,
                        low = alarmItem.low,
                        high = alarmItem.high,
                        type = alarmItem.type.value,
                        customDescription = alarmItem.customDescription,
                        mutedTill = alarmItem.mutedTill)
                    alarmItem.alarm?.enabled = alarmItem.isEnabled
                    alarmItem.alarm?.save()
                } else {
                    alarmItem.alarm?.enabled = alarmItem.isEnabled
                    alarmItem.alarm?.low = alarmItem.low
                    alarmItem.alarm?.high = alarmItem.high
                    alarmItem.alarm?.customDescription = alarmItem.customDescription
                    alarmItem.alarm?.mutedTill = alarmItem.mutedTill
                    alarmItem.alarm?.update()
                }
            } else if (alarmItem.alarm != null) {
                alarmItem.alarm?.enabled = false
                alarmItem.alarm?.mutedTill = alarmItem.mutedTill
                alarmItem.alarm?.update()
            }
            if (!alarmItem.isEnabled) {
                val notificationId = alarmItem.alarm?.id ?: -1
                removeNotificationById(notificationId)
            }
        }
    }

    fun claimSensor() {
        val sensorSettings = sensorSettings.value
        if (sensorSettings != null) {
            networkInteractor.claimSensor(sensorSettings) {
                updateNetworkStatus()
                if (it == null || it.error.isNullOrEmpty() == false) {
                    //TODO LOCALIZE
                    operationStatus.value = "Failed to claim tag: ${it?.error}"
                } else {
                    operationStatus.value = "Tag successfully claimed"
                }
            }
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun setName(name: String?) {
        interactor.updateTagName(sensorId, name)
        getTagInfo()
        if (networkInteractor.signedIn) {
            networkInteractor.updateSensor(sensorId)
        }
    }

    fun setupAlarmElements() {
        alarmElements.clear()

        with(alarmElements) {
            add(AlarmElement(
                AlarmType.TEMPERATURE,
                false,
                -40,
                85
            ))
            add(AlarmElement(
                AlarmType.HUMIDITY,
                false,
                0,
                100
            ))
            add(AlarmElement(
                AlarmType.PRESSURE,
                false,
                30000,
                110000
            ))
            add(AlarmElement(
                AlarmType.RSSI,
                false,
                -105,
                0
            ))
            add(AlarmElement(
                AlarmType.MOVEMENT,
                false,
                0,
                0
            ))
        }

        val dbAlarms = alarmRepository.getForSensor(sensorId)
        for (alarm in dbAlarms) {
            val item = alarmElements.firstOrNull { it.type.value == alarm.type }
            item?.let {
                item.high = alarm.high
                item.low = alarm.low
                item.isEnabled = alarm.enabled
                item.customDescription = alarm.customDescription ?: ""
                item.mutedTill = alarm.mutedTill
                item.alarm = alarm
                item.normalizeValues()
            }
        }
    }
}