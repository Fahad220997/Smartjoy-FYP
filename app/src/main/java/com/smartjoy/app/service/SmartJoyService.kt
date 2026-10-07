package com.smartjoy.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.smartjoy.app.logic.LocationUtils
import com.smartjoy.app.model.ZoneStatus
import com.smartjoy.app.model.ZoneType
import com.smartjoy.app.model.zooZones

class SmartJoyService : Service() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var lastZoneStatus = ZoneStatus.UNDEFINED

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { checkZones(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (hasLocationPermission()) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        }
    }

    private fun checkZones(location: Location) {
        var highestStatus = ZoneStatus.UNDEFINED
        var currentDangerZoneName: String? = null

        for (zone in zooZones) {
            val distance = LocationUtils.calculateDistance(zone.center, location.latitude, location.longitude)
            if (distance <= zone.radiusMeters) {
                val status = when (zone.type) {
                    ZoneType.DANGER -> ZoneStatus.DANGER
                    ZoneType.CAUTION -> ZoneStatus.CAUTION
                    ZoneType.SAFE -> ZoneStatus.SAFE
                }
                
                if (status == ZoneStatus.DANGER) {
                    highestStatus = ZoneStatus.DANGER
                    currentDangerZoneName = zone.name
                    break
                } else if (status == ZoneStatus.CAUTION && highestStatus != ZoneStatus.DANGER) {
                    highestStatus = ZoneStatus.CAUTION
                } else if (status == ZoneStatus.SAFE && highestStatus == ZoneStatus.UNDEFINED) {
                    highestStatus = ZoneStatus.SAFE
                }
            }
        }

        if (highestStatus == ZoneStatus.DANGER && lastZoneStatus != ZoneStatus.DANGER) {
            currentDangerZoneName?.let {
                SafetyNotificationHelper.showDangerAlert(this, it)
            }
        }
        lastZoneStatus = highestStatus
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "SmartJoy Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("SmartJoy Active")
            .setContentText("Monitoring child safety zones...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val SERVICE_CHANNEL_ID = "smartjoy_service_channel"
    }
}
