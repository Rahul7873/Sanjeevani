package com.example.sanjeevani

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class User_view : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_view)

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the Map Fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.Gmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Ambulance Login Button
        val ambulanceLogin = findViewById<ImageView>(R.id.AID_logIN)
        ambulanceLogin.setOnClickListener {
            startActivity(Intent(this, ALOGIN::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Basic Map Settings
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        // Check permission and enable blue dot
        try {
            mMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show()
        }

        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    updateUserMarker(userLocation)
                }
            }
        }, Looper.getMainLooper())
    }

    private fun updateUserMarker(latLng: LatLng) {
        if (userMarker == null) {
            // First time finding the user
            userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Your Location"))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            // Update existing marker position
            userMarker?.position = latLng
        }
    }
}