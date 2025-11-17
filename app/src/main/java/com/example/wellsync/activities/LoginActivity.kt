package com.example.wellsync.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wellsync.databinding.ActivityLoginBinding
import com.example.wellsync.utils.DataManager
import com.example.wellsync.utils.DataRecovery

class LoginActivity : AppCompatActivity() {

    private var binding: ActivityLoginBinding? = null
    private var dataManager: DataManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            binding?.let { setContentView(it.root) }


            if (DataRecovery.isDataCorrupted(this)) {
                Log.w("LoginActivity", "Corrupted data detected, clearing...")
                DataRecovery.clearAllAppData(this)
            }

            dataManager = DataManager(this)
            setupUI()

        } catch (e: Exception) {
            Log.e("LoginActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing login screen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        try {
            binding?.let { binding ->
                binding.btnLogin.setOnClickListener {
                    try {
                        val username = binding.etUsername.text?.toString()?.trim() ?: ""
                        val password = binding.etPassword.text?.toString() ?: ""

                        if (validateInput(username, password)) {
                            if (dataManager?.validateUser(username, password) == true) {
                                dataManager?.setCurrentUser(username)
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Error during login: ${e.message}", e)
                        Toast.makeText(this, "Login error occurred", Toast.LENGTH_SHORT).show()
                    }
                }

                binding.tvRegister.setOnClickListener {
                    try {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Error navigating to register: ${e.message}", e)
                        Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error setting up UI: ${e.message}", e)
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        return try {
            when {
                username.isEmpty() -> {
                    Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show()
                    false
                }
                password.isEmpty() -> {
                    Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> true
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error validating input: ${e.message}", e)
            false
        }
    }
}
