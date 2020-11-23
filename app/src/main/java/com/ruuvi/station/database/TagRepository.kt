package com.ruuvi.station.database

import android.content.Context
import androidx.annotation.NonNull
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.Alarm_Table
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.RuuviTagEntity_Table
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.database.tables.TagSensorReading_Table
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.TemperatureUnit

class TagRepository(
    private val preferences: Preferences,
    private val context: Context
) {

    fun getAllTags(isFavorite: Boolean): List<RuuviTagEntity> =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.favorite.eq(isFavorite))
            .orderBy(RuuviTagEntity_Table.createDate, true)
            .queryList()

    fun getTagById(id: String): RuuviTagEntity? =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.id.eq(id))
            .querySingle()

    fun deleteTagsAndRelatives(tag: RuuviTagEntity) {
        SQLite.delete(Alarm::class.java)
            .where(Alarm_Table.ruuviTagId.eq(tag.id))
            .execute()

        SQLite.delete(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(tag.id))
            .execute()

        tag.delete()
    }

    fun getTagReadings(tagId: String): List<TagSensorReading>? {
        return if (preferences.graphShowAllPoint) {
            TagSensorReading.getForTag(tagId, preferences.graphViewPeriod)
        } else {
            TagSensorReading.getForTagPruned(
                tagId,
                preferences.graphPointInterval,
                preferences.graphViewPeriod
            )
        }
    }

    fun updateTag(tag: RuuviTagEntity) {
        tag.update()
    }

    fun saveTag(@NonNull tag: RuuviTagEntity) {
        tag.save()
    }
}