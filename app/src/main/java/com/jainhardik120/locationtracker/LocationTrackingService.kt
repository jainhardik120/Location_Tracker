package com.jainhardik120.locationtracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.jainhardik120.locationtracker.data.LatestLocationResponse
import com.jainhardik120.locationtracker.data.LocationAPI
import com.jainhardik120.locationtracker.data.LocationData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "LocationTrackingChannel"
        private const val NOTIFICATION_ID = 123
        private const val TOKEN_KEY = "TOKEN"
        private const val ID_KEY = "ID"

        const val START_ACTION = "start"
        const val STOP_ACTION = "stop"
    }

    @Inject
    lateinit var locationAPI: LocationAPI

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener {
            it?.let {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
                calendar.add(Calendar.HOUR_OF_DAY, 1)
                val expiryTimeDate = calendar.time

                scope.launch {
                    when (val result = locationAPI.createNewLocation(
                        LocationData(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            expiryTime = expiryTimeDate.toString()
                        )
                    )) {
                        is Result.ClientException -> {}
                        is Result.Exception -> {}
                        is Result.Success -> {
                            result.data?.let {
                                with(sharedPreferences.edit()) {
                                    putString(TOKEN_KEY, it.token)
                                    putString(ID_KEY, it.id)
                                }.apply()
                                startLocationUpdates()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_ACTION -> {
                createNotificationChannel()
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
            }
            STOP_ACTION -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Location Tracking"
            val descriptionText = "Keep track of user's location"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Location Tracking")
            .setContentText("Tracking your location...")
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    private fun startLocationUpdates() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {
                    updateLocationToServer(it.latitude, it.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun checkToken(): Boolean {
        return sharedPreferences.contains(TOKEN_KEY)
    }

    private fun updateLocationToServer(latitude: Double, longitude: Double) {
        if (!checkToken()) {
            return
        }
        scope.launch {
            when (locationAPI.updateLocation(
                sharedPreferences.getString(TOKEN_KEY, null) ?: return@launch,
                data = LatestLocationResponse(
                    latitude, longitude
                )
            )) {
                is Result.ClientException -> {

                }

                is Result.Exception -> {

                }

                is Result.Success -> {

                }
            }

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}