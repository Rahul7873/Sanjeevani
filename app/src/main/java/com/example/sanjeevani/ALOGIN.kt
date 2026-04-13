package com.example.sanjeevani

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        // Search for the specific A-ID in your Firebase node
        database.child(aid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get the actual password stored in Firebase for this AID
                    val dbPassword = snapshot.child("password").value.toString()

                    if (dbPassword == pass) {
                        val isAlreadyLoggedIn = snapshot.child("isLoggedIn").getValue(Boolean::class.java) ?: false
                        
                        if (isAlreadyLoggedIn) {
                            Toast.makeText(this@ALOGIN, "This AID is already logged in on another device", Toast.LENGTH_LONG).show()
                        } else {
                            // Mark as logged in in Firebase
                            database.child(aid).child("isLoggedIn").setValue(true)
                            
                            // SUCCESS: Save login state and move to MainActivity
                            saveLoginStatus(aid)

                            Toast.makeText(this@ALOGIN, "Login Successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@ALOGIN, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@ALOGIN, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ALOGIN, "A-ID not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ALOGIN, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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