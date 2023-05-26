package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.util.BackgroundScanModes
import timber.log.Timber

class TagInteractor constructor(
    private val tagRepository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val tagSettingsInteractor: TagSettingsInteractor
) {

    fun getTags(): List<RuuviTag> =
        tagRepository
            .getFavoriteSensors()
            .map { it.copy(alarmSensorStatus = alarmCheckInteractor.getAlarmStatus(it)) }
            .also { Timber.d("TagInteractor - getTags") }

    fun getTagEntities(isFavorite: Boolean = true): List<RuuviTagEntity> =
        tagRepository.getAllTags(isFavorite)
            .also {Timber.d("TagInteractor - getTagEntities")}

    fun getTagEntityById(tagId: String): RuuviTagEntity? =
        tagRepository.getTagById(tagId)

    fun getTagByID(tagId: String): RuuviTag? =
        tagRepository.getFavoriteSensorById(tagId)

    fun getBackgroundScanMode(): BackgroundScanModes =
        preferencesRepository.getBackgroundScanMode()

    fun setBackgroundScanMode(mode: BackgroundScanModes) {
        preferencesRepository.setBackgroundScanMode(mode)
    }

    fun isFirstGraphVisit(): Boolean =
        preferencesRepository.isFirstGraphVisit()

    fun setIsFirstGraphVisit(isFirst: Boolean) =
        preferencesRepository.setIsFirstGraphVisit(isFirst)

    fun getHistoryLength(): Long = sensorHistoryRepository.countAll()

    fun makeSensorFavorite(sensor: RuuviTagEntity) {
        sensor.id?.let { sensorId ->
            tagRepository.makeSensorFavorite(sensor)
            tagSettingsInteractor.setRandomDefaultBackgroundImage(sensorId)
        }
    }
}