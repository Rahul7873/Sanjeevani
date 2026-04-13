package com.example.sanjeevani

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class User_view : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userMarker: Marker? = null
    private var ambulanceMarker: Marker? = null
    private var currentUserLocation: LatLng? = null
    
    private val database = FirebaseDatabase.getInstance().getReference("LiveAmbulance")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.Gmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageView>(R.id.AID_logIN).setOnClickListener {
            startActivity(Intent(this, ALOGIN::class.java))
        }

        startAmbulanceService()
        listenToAmbulance()
    }

    private fun startAmbulanceService() {
        val intent = Intent(this, AmbulanceMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun listenToAmbulance() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)
                val status = snapshot.child("status").getValue(String::class.java)

                if (lat != null && lng != null && status == "active") {
                    val ambulanceLoc = LatLng(lat, lng)
                    updateAmbulanceMarker(ambulanceLoc)
                } else {
                    ambulanceMarker?.remove()
                    ambulanceMarker = null
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateAmbulanceMarker(latLng: LatLng) {
        if (!::mMap.isInitialized) return

        if (ambulanceMarker == null) {
            ambulanceMarker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title("Ambulance SOS")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        } else {
            ambulanceMarker?.position = latLng
        }
    }

    private fun checkDistanceAndVibrate(ambulanceLoc: LatLng) {
        currentUserLocation?.let { userLoc ->
            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                ambulanceLoc.latitude, ambulanceLoc.longitude,
                results
            )
            val distance = results[0]
            if (distance <= 150.0) {
                vibratePhone()
                Toast.makeText(this, "AMBULANCE NEARBY!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1500)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        try { mMap.isMyLocationEnabled = true } catch (e: SecurityException) {}
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { 
                    currentUserLocation = LatLng(it.latitude, it.longitude)
                    updateUserMarker(currentUserLocation!!)
                }
            }
        }, Looper.getMainLooper())
    }

    private fun updateUserMarker(latLng: LatLng) {
        if (userMarker == null) {
            userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Your Location"))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            userMarker?.position = latLng
        }
    }
}
