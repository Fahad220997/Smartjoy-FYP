package com.smartjoy.app.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.smartjoy.app.logic.LocationUtils
import com.smartjoy.app.model.Zone
import com.smartjoy.app.model.ZoneProximityStatus
import com.smartjoy.app.model.ZoneStatus
import com.smartjoy.app.model.ZoneType
import com.smartjoy.app.model.ZoneUiState
import com.smartjoy.app.model.ZooLocation
import com.smartjoy.app.model.zooZones
import com.smartjoy.app.service.SafetyNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DangerZoneViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(ZoneUiState(
        zoneStatuses = zooZones.map { ZoneProximityStatus(it, Float.NaN, false) }
    ))
    val uiState: StateFlow<ZoneUiState> = _uiState.asStateFlow()

    private var lastStatus = ZoneStatus.UNDEFINED

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onNewLocation(it) }
        }
    }

    @SuppressLint("MissingPermission")
    fun startMonitoring() {
        if (_uiState.value.isMonitoring || !hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, getApplication<Application>().mainLooper)
        _uiState.value = _uiState.value.copy(isMonitoring = true)
    }

    fun stopMonitoring() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _uiState.value = _uiState.value.copy(isMonitoring = false)
    }

    private fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun onNewLocation(location: Location) {
        val proximityStatuses = zooZones.map { zone ->
            val distance = LocationUtils.calculateDistance(zone.center, location.latitude, location.longitude)
            ZoneProximityStatus(zone, distance, distance <= zone.radiusMeters)
        }

        // Determine current status based on priority: DANGER > CAUTION > SAFE
        val activeProximities = proximityStatuses.filter { it.isInside }
        
        val newStatus = when {
            activeProximities.any { it.zone.type == ZoneType.DANGER } -> ZoneStatus.DANGER
            activeProximities.any { it.zone.type == ZoneType.CAUTION } -> ZoneStatus.CAUTION
            activeProximities.any { it.zone.type == ZoneType.SAFE } -> ZoneStatus.SAFE
            else -> ZoneStatus.UNDEFINED
        }

        val activeZone = activeProximities
            .sortedBy { 
                when(it.zone.type) {
                    ZoneType.DANGER -> 0
                    ZoneType.CAUTION -> 1
                    ZoneType.SAFE -> 2
                }
            }
            .firstOrNull()?.zone

        // Notification logic
        if (newStatus == ZoneStatus.DANGER && lastStatus != ZoneStatus.DANGER) {
            activeZone?.let {
                SafetyNotificationHelper.showDangerAlert(getApplication(), it.name)
            }
        }
        lastStatus = newStatus

        _uiState.value = _uiState.value.copy(
            currentLocation = ZooLocation(location.latitude, location.longitude),
            zoneStatuses = proximityStatuses,
            currentZoneStatus = newStatus,
            activeZone = activeZone
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 3000L
    }
}
