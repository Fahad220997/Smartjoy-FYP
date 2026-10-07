package com.smartjoy.app.model

import android.graphics.Color
import androidx.annotation.StringRes
import com.smartjoy.app.R

enum class ZoneType(
    @StringRes val labelResId: Int, 
    val borderColor: Int, 
    val fillColor: Int
) {
    DANGER(R.string.zone_type_danger, Color.RED, Color.argb(80, 255, 0, 0)),
    CAUTION(R.string.zone_type_caution, Color.rgb(255, 160, 0), Color.argb(80, 255, 160, 0)),
    SAFE(R.string.zone_type_safe, Color.GREEN, Color.argb(80, 0, 255, 0))
}

data class Zone(
    val id: String,
    val name: String,
    val center: ZooLocation,
    val radiusMeters: Double,
    val type: ZoneType
)

enum class ZoneStatus { DANGER, CAUTION, SAFE, UNDEFINED }

data class ZoneProximityStatus(
    val zone: Zone,
    val distanceMeters: Float,
    val isInside: Boolean
)

data class ZoneUiState(
    val isMonitoring: Boolean = false,
    val currentLocation: ZooLocation? = null,
    val zoneStatuses: List<ZoneProximityStatus> = emptyList(),
    val currentZoneStatus: ZoneStatus = ZoneStatus.UNDEFINED,
    val activeZone: Zone? = null
)

/**
 * Sample zone data for Peshawar Zoo.
 */
val zooZones = listOf(
    Zone(
        id = "lion_house",
        name = "Lion House",
        center = ZooLocation(34.017506, 71.489622),
        radiusMeters = 25.0,
        type = ZoneType.DANGER
    ),
    Zone(
        id = "tiger_enclosure",
        name = "Tiger Enclosure",
        center = ZooLocation(34.017806, 71.490422),
        radiusMeters = 20.0,
        type = ZoneType.DANGER
    ),
    Zone(
        id = "water_feature",
        name = "Central Pond Area",
        center = ZooLocation(34.017306, 71.490822),
        radiusMeters = 30.0,
        type = ZoneType.CAUTION
    ),
    Zone(
        id = "main_path_bottleneck",
        name = "Busy Walkway",
        center = ZooLocation(34.017106, 71.490322),
        radiusMeters = 15.0,
        type = ZoneType.CAUTION
    ),
    Zone(
        id = "play_area",
        name = "Designated Play Area",
        center = ZooLocation(34.016906, 71.489822),
        radiusMeters = 40.0,
        type = ZoneType.SAFE
    )
)
