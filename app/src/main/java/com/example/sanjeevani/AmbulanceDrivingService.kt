package com.example.sanjeevani

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class AmbulanceDrivingService : Service() {

    private val CHANNEL_ID = "AmbulanceDrivingChannel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val database = FirebaseDatabase.getInstance().getReference("LiveAmbulance")
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2, createNotification("Emergency SOS active - Sharing live location"))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Ambulance Driving Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SANJEEVANI DRIVER")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { 
                    val locationMap = mapOf(
                        "lat" to it.latitude,
                        "lng" to it.longitude,
                        "status" to "active"
                    )
                    database.setValue(locationMap)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    override fun onDestroy() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        database.child("status").setValue("inactive")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
