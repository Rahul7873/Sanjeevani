package com.example.sanjeevani

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.Gmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupSwapLogic()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // We can safely enable this because permissions were checked in the previous activity
        try {
            mMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    updateUI(LatLng(it.latitude, it.longitude))
                }
            }
        }, Looper.getMainLooper())
    }

    private fun updateUI(latLng: LatLng) {
        if (myMarker == null) {
            myMarker = mMap.addMarker(MarkerOptions().position(latLng).title("You are here"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        } else {
            myMarker?.position = latLng
        }
    }

    private fun setupSwapLogic() {
        val etFrom = findViewById<EditText>(R.id.etFrom)
        val etTo = findViewById<EditText>(R.id.etTo)
        val ivSwap = findViewById<ImageView>(R.id.ivSwap)

        ivSwap.setOnClickListener {
            val fromText = etFrom.text.toString()
            val toText = etTo.text.toString()

            etFrom.setText(toText)
            etTo.setText(fromText)

            etFrom.setSelection(etFrom.text.length)
            etTo.setSelection(etTo.text.length)
        }
    }
}