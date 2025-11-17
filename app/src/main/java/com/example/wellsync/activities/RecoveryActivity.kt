package com.example.wellsync.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.wellsync.R
import com.example.wellsync.utils.DataRecovery

class RecoveryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = "Recovering WellSync...\nPlease wait..."
            textSize = 18f
            setPadding(50, 200, 50, 50)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        setContentView(textView)

        performRecovery()
    }

    private fun performRecovery() {
        Thread {
            try {
                DataRecovery.clearAllAppData(this)

                Thread.sleep(1000)

                runOnUiThread {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }.start()
    }
}
