package com.smartjoy.app.logic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import com.smartjoy.app.model.BleDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BleScanner(private val bluetoothAdapter: BluetoothAdapter?) {

    @SuppressLint("MissingPermission")
    fun scanDevices(): Flow<BleDevice> = callbackFlow {
        if (bluetoothAdapter == null) {
            close(IllegalStateException("This device does not support Bluetooth."))
            return@callbackFlow
        }
        if (!bluetoothAdapter.isEnabled) {
            close(IllegalStateException("Bluetooth is turned off. Turn it on to scan for the wearable."))
            return@callbackFlow
        }
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            close(IllegalStateException("Bluetooth LE scanning is unavailable on this device."))
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                trySend(
                    BleDevice(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        rssi = result.rssi
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed with error code $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, callback)

        awaitClose { scanner.stopScan(callback) }
    }
}
