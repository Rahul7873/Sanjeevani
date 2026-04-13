package com.example.sanjeevani

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class permission_user : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    private val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!locationGranted) {
            Toast.makeText(this, "Permission denied. You can still enter details, but map features won't work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Shared Pref Check (Always first)
        val sharedPref = getSharedPreferences("SanjeevaniPrefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("isLoggedIn", false)) {
            val userType = sharedPref.getString("userType", "Normal")
            if (userType == "Ambulance") {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, User_view::class.java))
            }
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_permission_user)

        // 2. TRIGGER PERMISSIONS IMMEDIATELY
        checkPermissionsOnly()

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance().getReference("Users")

        val firstNameInput = findViewById<EditText>(R.id.Lname)
        val lastNameInput = findViewById<EditText>(R.id.Fname)
        val continueButton = findViewById<MaterialButton>(R.id.nter)
        val aidTextView = findViewById<TextView>(R.id.AID)

        // 3. Button now only handles saving data
        continueButton.setOnClickListener {
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                saveDataToFirebase(firstName, lastName)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        aidTextView.setOnClickListener {
            startActivity(Intent(this, ALOGIN::class.java))
        }
    }

    private fun checkPermissionsOnly() {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun saveDataToFirebase(firstName: String, lastName: String) {
        val userId = database.push().key ?: ""
        val userMap = mapOf("firstName" to firstName, "lastName" to lastName)

        database.child(userId).setValue(userMap).addOnSuccessListener {
            val sharedPref = getSharedPreferences("SanjeevaniPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putBoolean("isLoggedIn", true)
                putString("userName", firstName)
                apply()
            }
            startActivity(Intent(this, User_view::class.java))
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}