package com.example.sanjeevani

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myMarker: Marker? = null // This will be your moving pin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the GPS tool
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.Gmap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Start tracking the location
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission") // Ensure you have asked for permissions first!
    private fun startLocationUpdates() {
        // Create a request: Check location every 2 seconds
        // Change RenderScript.Priority to Priority
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)


                    // 1. Move or Add the Marker
                    if (myMarker == null) {
                        myMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                    } else {
                        myMarker?.position = currentLatLng
                    }

                    // 2. NAVIGATE: Always move the camera to follow the marker
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                }
            }
        }, Looper.getMainLooper())
    }
}