package com.smartjoy.app.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartjoy.app.logic.BleScanner
import com.smartjoy.app.model.BleDevice
import com.smartjoy.app.model.ProximityLevel
import com.smartjoy.app.model.TrackingUiState
import com.smartjoy.app.model.rssiToProximity
import com.smartjoy.app.service.SafetyNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChildTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager =
        application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val scanner = BleScanner(bluetoothManager?.adapter)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var timeoutJob: Job? = null

    init {
        val savedAddress = prefs.getString(KEY_PAIRED_ADDRESS, null)
        if (savedAddress != null) {
            val savedName = prefs.getString(KEY_PAIRED_NAME, null) ?: "Child's Wearable"
            _uiState.value = _uiState.value.copy(
                pairedDevice = BleDevice(name = savedName, address = savedAddress, rssi = UNKNOWN_RSSI)
            )
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isScanning = true, nearbyDevices = emptyList(), errorMessage = null)
        scanJob = viewModelScope.launch {
            try {
                scanner.scanDevices().collect { device ->
                    val paired = _uiState.value.pairedDevice
                    if (paired != null && device.address == paired.address) {
                        onPairedDeviceSeen(device.rssi)
                    }
                    val others = _uiState.value.nearbyDevices.filterNot { it.address == device.address }
                    _uiState.value = _uiState.value.copy(
                        nearbyDevices = (others + device).sortedByDescending { it.rssi }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Bluetooth scan failed")
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun pairDevice(device: BleDevice) {
        prefs.edit()
            .putString(KEY_PAIRED_ADDRESS, device.address)
            .putString(KEY_PAIRED_NAME, device.name)
            .apply()
        _uiState.value = _uiState.value.copy(pairedDevice = device)
        updateProximity(rssiToProximity(device.rssi))
        resetTimeout()
    }

    fun unpairDevice() {
        prefs.edit().remove(KEY_PAIRED_ADDRESS).remove(KEY_PAIRED_NAME).apply()
        timeoutJob?.cancel()
        _uiState.value = _uiState.value.copy(pairedDevice = null, proximity = ProximityLevel.OUT_OF_RANGE)
    }

    private fun onPairedDeviceSeen(rssi: Int) {
        val paired = _uiState.value.pairedDevice ?: return
        _uiState.value = _uiState.value.copy(pairedDevice = paired.copy(rssi = rssi))
        updateProximity(rssiToProximity(rssi))
        resetTimeout()
    }

    private fun resetTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(DEVICE_TIMEOUT_MS)
            updateProximity(ProximityLevel.OUT_OF_RANGE)
        }
    }

    private fun updateProximity(newLevel: ProximityLevel) {
        val previous = _uiState.value.proximity
        _uiState.value = _uiState.value.copy(proximity = newLevel)

        val wasSafe = previous == ProximityLevel.NEAR || previous == ProximityLevel.SAFE
        val isNowUnsafe = newLevel == ProximityLevel.FAR || newLevel == ProximityLevel.OUT_OF_RANGE
        if (wasSafe && isNowUnsafe) {
            val deviceName = _uiState.value.pairedDevice?.name ?: "Child's wearable"
            SafetyNotificationHelper.showOutOfRangeAlert(getApplication(), deviceName)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        timeoutJob?.cancel()
    }

    companion object {
        private const val PREFS_NAME = "smartjoy_tracking"
        private const val KEY_PAIRED_ADDRESS = "paired_address"
        private const val KEY_PAIRED_NAME = "paired_name"
        private const val DEVICE_TIMEOUT_MS = 15_000L

        /** Placeholder RSSI for a paired device that hasn't been seen by a scan yet this session. */
        const val UNKNOWN_RSSI = Int.MIN_VALUE
    }
}
