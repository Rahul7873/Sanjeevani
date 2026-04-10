package com.example.sanjeevani

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivLogo)
        val title = findViewById<TextView>(R.id.tvTitle)
        val caption = findViewById<TextView>(R.id.tvCaption)

        // Set initial state for animation (invisible and slightly lower)
        logo.alpha = 0f
        logo.translationY = 50f
        title.alpha = 0f
        caption.alpha = 0f

        // Start Animations
        logo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate().alpha(1f).setDuration(1000).setStartDelay(300).start()
        caption.animate().alpha(1f).setDuration(1000).setStartDelay(600).start()

        // Transition to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Smooth transition between activities
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500) // Increased to 2.5s so user can see the animation
    }
}