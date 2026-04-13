package com.example.sanjeevani

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ALOGIN : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alogin)

        // 1. Initialize Firebase Reference
        database = FirebaseDatabase.getInstance().getReference("AmbulanceData")

        // 2. Initialize Views
        val aidInput = findViewById<EditText>(R.id.AID)
        val pwdInput = findViewById<EditText>(R.id.PWD)
        val loginBtn = findViewById<MaterialButton>(R.id.login)
        val normalUserText = findViewById<TextView>(R.id.normal_user)

        // 3. Login Button Logic
        loginBtn.setOnClickListener {
            val enteredAID = aidInput.text.toString().trim()
            val enteredPWD = pwdInput.text.toString().trim()

            if (enteredAID.isNotEmpty() && enteredPWD.isNotEmpty()) {
                loginAmbulance(enteredAID, enteredPWD)
            } else {
                Toast.makeText(this, "Please enter A-ID and Password", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Back to Normal User Logic
        normalUserText.setOnClickListener {
            val intent = Intent(this, permission_user::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loginAmbulance(aid: String, pass: String) {
        val loginBtn = findViewById<MaterialButton>(R.id.login)
        loginBtn.isEnabled = false // Disable immediately to prevent multiple clicks and crashes

        try {
            val currentDeviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

            // Search for the specific A-ID in your Firebase node
            database.child(aid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (snapshot.exists()) {
                            val dbPassword = snapshot.child("password").getValue(String::class.java)

                            if (dbPassword == pass) {
                                // Safe check for isLoggedIn (handles boolean or string "true")
                                val isAlreadyLoggedIn = snapshot.child("isLoggedIn").value?.toString()?.toBoolean() ?: false
                                val lastDeviceId = snapshot.child("deviceId").getValue(String::class.java) ?: ""

                                // Robust check: only block if logged in on a DIFFERENT, NON-EMPTY device ID
                                if (isAlreadyLoggedIn && lastDeviceId.isNotEmpty() && lastDeviceId != currentDeviceId) {
                                    Toast.makeText(this@ALOGIN, "This AID is already in use on another device", Toast.LENGTH_LONG).show()
                                    loginBtn.isEnabled = true
                                } else {
                                    // Update login status and Device ID
                                    val updates = mapOf(
                                        "isLoggedIn" to true,
                                        "deviceId" to currentDeviceId
                                    )
                                    database.child(aid).updateChildren(updates).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            saveLoginStatus(aid)
                                            Toast.makeText(this@ALOGIN, "Login Successful!", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this@ALOGIN, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            loginBtn.isEnabled = true
                                            Toast.makeText(this@ALOGIN, "Database update failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                loginBtn.isEnabled = true
                                Toast.makeText(this@ALOGIN, "Incorrect Password", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            loginBtn.isEnabled = true
                            Toast.makeText(this@ALOGIN, "A-ID not found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        loginBtn.isEnabled = true
                        Toast.makeText(this@ALOGIN, "Error processing data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loginBtn.isEnabled = true
                    Toast.makeText(this@ALOGIN, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            loginBtn.isEnabled = true
            Toast.makeText(this@ALOGIN, "System error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLoginStatus(aid: String) {
        val sharedPref = getSharedPreferences("SanjeevaniPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("userType", "Ambulance")
        editor.putString("currentAID", aid)
        editor.apply()
    }
}