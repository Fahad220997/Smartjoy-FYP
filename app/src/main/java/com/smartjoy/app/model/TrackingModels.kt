package com.smartjoy.app.model

import androidx.annotation.StringRes
import com.smartjoy.app.R

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

enum class ProximityLevel(@StringRes val labelResId: Int) {
    NEAR(R.string.proximity_near),
    SAFE(R.string.proximity_safe),
    FAR(R.string.proximity_far),
    OUT_OF_RANGE(R.string.proximity_out_of_range)
}

data class TrackingUiState(
    val isScanning: Boolean = false,
    val nearbyDevices: List<BleDevice> = emptyList(),
    val pairedDevice: BleDevice? = null,
    val proximity: ProximityLevel = ProximityLevel.OUT_OF_RANGE,
    val errorMessage: String? = null
)

/**
 * BLE RSSI is a very rough distance proxy (affected by obstacles, device orientation, etc.),
 * but it's the standard low-cost approach for consumer proximity wearables.
 */
fun rssiToProximity(rssi: Int?): ProximityLevel = when {
    rssi == null -> ProximityLevel.OUT_OF_RANGE
    rssi >= -60 -> ProximityLevel.NEAR
    rssi >= -75 -> ProximityLevel.SAFE
    rssi >= -90 -> ProximityLevel.FAR
    else -> ProximityLevel.OUT_OF_RANGE
}
