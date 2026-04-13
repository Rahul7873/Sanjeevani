package com.example.sanjeevani

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.*

class AmbulanceMonitorService : Service() {

    private val CHANNEL_ID = "AmbulanceMonitorChannel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentUserLocation: Location? = null
    private val database = FirebaseDatabase.getInstance().getReference("LiveAmbulance")

    @Volatile private var isVibrating = false
    @Volatile private var isDismissed = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("Monitoring for nearby ambulances..."))
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        listenToAmbulance()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Ambulance Monitor Service Channel",
                NotificationManager.IMPORTANCE_HIGH // High importance to allow vibration and sound
            ).apply {
                description = "Alerts for nearby ambulances"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(null, null) 
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Visible on lockscreen
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        // Intent to open the app (Content Intent)
        // Removed the extra "STOP_VIBRATION" flag from here
        val notificationIntent = Intent(this, User_view::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent for the explicit STOP button action
        val stopIntent = Intent(this, AmbulanceMonitorService::class.java).apply {
            action = "STOP_VIBRATION"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SANJEEVANI ALERT")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent) // Proper Activity intent
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)

        // Always show stop button if we are currently vibrating
        if (isVibrating) {
            builder.addAction(android.R.drawable.ic_delete, "STOP VIBRATION", stopPendingIntent)
        }

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentUserLocation = locationResult.lastLocation
            }
        }, Looper.getMainLooper())
    }

    private fun listenToAmbulance() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                val status = snapshot.child("status").getValue(String::class.java)

                if (lat != null && lng != null && status == "active") {
                    checkDistanceAndVibrate(lat, lng)
                } else {
                    // Reset dismissed state if ambulance is no longer active
                    isDismissed = false
                    // Stop vibration if driver stops or ambulance is inactive
                    if (isVibrating) {
                        stopVibration()
                        isVibrating = false
                        updateNotification("Monitoring for nearby ambulances...")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkDistanceAndVibrate(ambLat: Double, ambLng: Double) {
        currentUserLocation?.let { userLoc ->
            val ambLoc = Location("").apply {
                latitude = ambLat
                longitude = ambLng
            }
            val distance = userLoc.distanceTo(ambLoc)
            if (distance <= 150.0) { 
                if (!isVibrating && !isDismissed) {
                    vibratePhone()
                    isVibrating = true
                    updateNotification("AMBULANCE NEARBY! Clear the way!")
                }
            } else {
                // Out of range: stop vibration and reset dismissal so they can be alerted again later
                if (isVibrating) {
                    stopVibration()
                    isVibrating = false
                }
                isDismissed = false 
                updateNotification("Monitoring for nearby ambulances...")
            }
        }
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, createNotification(content))
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sanjeevani:VibrationWakeLock")
        wakeLock.acquire(3000)

        val pattern = longArrayOf(0, 1000, 500) // 1s vibrate, 0.5s pause
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat from index 0
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
        
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun stopVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_VIBRATION") {
            if (isVibrating) {
                stopVibration()
                isVibrating = false
                isDismissed = true
                updateNotification("Alert dismissed. Monitoring...")
            }
        }
        return START_STICKY
    }
}
