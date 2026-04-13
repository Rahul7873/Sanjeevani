package com.example.sanjeevani

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.widget.AutoCompleteTextView
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.PlacesClient

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myMarker: Marker? = null
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Places
        if (!Places.isInitialized()) {
            val apiKey = getApiKeyFromManifest()
            // Standard initialization works with "Places API"
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.Gmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupSwapLogic()
        setupAutocompleteFields()
    }

    private fun getApiKeyFromManifest(): String {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun setupAutocompleteFields() {
        val etFrom = findViewById<AutoCompleteTextView>(R.id.etFrom)
        val etTo = findViewById<AutoCompleteTextView>(R.id.etTo)

        // Ensure you have the PlaceAutocompleteAdapter class in your project
        val adapter = PlaceAutocompleteAdapter(this, placesClient)

        etFrom.setAdapter(adapter)
        etTo.setAdapter(adapter)

        etFrom.setOnItemClickListener { parent, _, position, _ ->
            val prediction = parent.getItemAtPosition(position) as AutocompletePrediction
            etFrom.setText(prediction.getFullText(null))
            etFrom.setSelection(etFrom.text.length)
        }

        etTo.setOnItemClickListener { parent, _, position, _ ->
            val prediction = parent.getItemAtPosition(position) as AutocompletePrediction
            etTo.setText(prediction.getFullText(null))
            etTo.setSelection(etTo.text.length)
        }
    }

    // --- EXISTING MAP LOGIC ---

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
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
        val etFrom = findViewById<AutoCompleteTextView>(R.id.etFrom)
        val etTo = findViewById<AutoCompleteTextView>(R.id.etTo)
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