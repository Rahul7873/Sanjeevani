package com.example.sanjeevani

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myMarker: Marker? = null
    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null
    private var polyline: Polyline? = null
    
    private var fromLatLng: LatLng? = null
    private var toLatLng: LatLng? = null
    private var isDrivingMode = false
    
    private lateinit var placesClient: PlacesClient
    private lateinit var tvDistance: TextView
    private lateinit var autocompleteAdapter: PlaceAutocompleteAdapter
    private val database = FirebaseDatabase.getInstance().getReference("LiveAmbulance")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDistance = findViewById(R.id.tvDistance)

        if (!Places.isInitialized()) {
            val apiKey = getApiKeyFromManifest()
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.Gmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupAutocompleteFields()
        setupSosButton()
        
        findViewById<ImageView>(R.id.ivSwap).visibility = View.GONE
    }

    private fun getApiKeyFromManifest(): String {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) { "" }
    }

    private fun setupSosButton() {
        val btnSos = findViewById<MaterialButton>(R.id.btn_Sos)
        btnSos.setOnClickListener {
            if (toLatLng == null) {
                Toast.makeText(this, "Please select a destination first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isDrivingMode = !isDrivingMode
            if (isDrivingMode) {
                btnSos.text = "STOP"
                btnSos.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
                Toast.makeText(this, "Driving Mode Started", Toast.LENGTH_SHORT).show()
                
                // Start background service for driving mode
                val serviceIntent = Intent(this, AmbulanceDrivingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                btnSos.text = "SOS"
                btnSos.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF3D00"))
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(fromLatLng!!).zoom(15f).tilt(0f).bearing(0f).build()
                ))
                
                // Stop background service
                stopService(Intent(this, AmbulanceDrivingService::class.java))
            }
        }
    }

    private fun setupAutocompleteFields() {
        val etTo = findViewById<AutoCompleteTextView>(R.id.etTo)
        autocompleteAdapter = PlaceAutocompleteAdapter(this, placesClient)
        etTo.setAdapter(autocompleteAdapter)

        etTo.setOnItemClickListener { parent, _, position, _ ->
            val prediction = parent.getItemAtPosition(position) as AutocompletePrediction
            etTo.setText(prediction.getFullText(null))
            fetchPlaceDetails(prediction.placeId) { latLng ->
                toLatLng = latLng
                updateMarkersAndPath()
            }
        }
    }

    private fun fetchPlaceDetails(placeId: String, callback: (LatLng) -> Unit) {
        val fields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, fields)
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            response.place.latLng?.let { callback(it) }
        }
    }

    private fun updateMarkersAndPath() {
        fromLatLng?.let { latLng ->
            fromMarker?.remove()
            fromMarker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        }

        toLatLng?.let { latLng ->
            toMarker?.remove()
            toMarker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        }

        if (fromLatLng != null && toLatLng != null) {
            getDirections(fromLatLng!!, toLatLng!!)
            if (!isDrivingMode) {
                val bounds = LatLngBounds.Builder().include(fromLatLng!!).include(toLatLng!!).build()
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            }
        }
    }

    private fun getDirections(origin: LatLng, dest: LatLng) {
        val apiKey = getApiKeyFromManifest()
        val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&key=$apiKey"

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            try {
                val response = URL(urlString).readText()
                val jsonResponse = JSONObject(response)
                val routes = jsonResponse.getJSONArray("routes")
                
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val legs = route.getJSONArray("legs")
                    val leg = legs.getJSONObject(0)
                    val distanceText = leg.getJSONObject("distance").getString("text")
                    val durationText = leg.getJSONObject("duration").getString("text")
                    val polylineEncoded = route.getJSONObject("overview_polyline").getString("points")
                    
                    val decodedPath = PolyUtil.decode(polylineEncoded)

                    handler.post {
                        polyline?.remove()
                        polyline = mMap.addPolyline(PolylineOptions()
                            .addAll(decodedPath)
                            .width(15f)
                            .color(Color.parseColor("#4285F4"))
                            .geodesic(true))
                            
                        tvDistance.text = "Distance: $distanceText | Time: $durationText"
                        tvDistance.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("Directions", "Error: ${e.message}")
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = true
        try { mMap.isMyLocationEnabled = true } catch (e: SecurityException) {}
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { 
                    val latLng = LatLng(it.latitude, it.longitude)
                    val bearing = it.bearing
                    fromLatLng = latLng
                    
                    if (::autocompleteAdapter.isInitialized) {
                        autocompleteAdapter.setBiasLocation(latLng)
                    }

                    updateUI(latLng, bearing)
                    
                    // The service now handles background updates to Firebase
                }
            }
        }, Looper.getMainLooper())
    }

    private fun updateUI(latLng: LatLng, bearing: Float) {
        if (myMarker == null) {
            myMarker = mMap.addMarker(MarkerOptions().position(latLng).title("You are here"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        } else {
            myMarker?.position = latLng
        }

        if (isDrivingMode) {
            val cameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(18f)
                .tilt(45f)
                .bearing(bearing)
                .build()
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            if (toLatLng != null) getDirections(latLng, toLatLng!!)
        }
    }
}
