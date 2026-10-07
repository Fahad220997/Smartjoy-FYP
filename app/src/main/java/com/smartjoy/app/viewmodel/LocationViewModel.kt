package com.smartjoy.app.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.smartjoy.app.model.ZooLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _currentLocation = MutableStateFlow<ZooLocation?>(null)
    val currentLocation: StateFlow<ZooLocation?> = _currentLocation.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(hasPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) fetchLastLocation()
    }

    private fun hasPermission(): Boolean {
        val context = getApplication<Application>()
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun fetchLastLocation() {
        if (!hasPermission()) return
        viewModelScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    _currentLocation.value = ZooLocation(location.latitude, location.longitude)
                }
            } catch (_: Exception) {
                // Location unavailable
            }
        }
    }
}
