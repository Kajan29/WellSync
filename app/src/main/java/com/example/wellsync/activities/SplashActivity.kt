package com.example.wellsync.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.wellsync.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_splash)

            clearAllData()

            Handler(Looper.getMainLooper()).postDelayed({
                navigateToLogin()
            }, 2000)

        } catch (e: Exception) {
            Log.e("SplashActivity", "Error in onCreate: ${e.message}", e)
            navigateToLogin()
        }
    }

    private fun clearAllData() {
        try {
            val sharedPrefs = getSharedPreferences("wellsync_prefs", MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()

            cacheDir.deleteRecursively()

            Log.d("SplashActivity", "All data cleared successfully")
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error clearing data: ${e.message}", e)
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("SplashActivity", "Critical error - cannot navigate: ${e.message}", e)
            finishAffinity()
        }
    }
}
