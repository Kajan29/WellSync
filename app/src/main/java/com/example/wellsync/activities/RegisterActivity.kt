package com.example.wellsync.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wellsync.databinding.ActivityRegisterBinding
import com.example.wellsync.models.User
import com.example.wellsync.utils.DataManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)
        setupUI()
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val email = binding.etEmail.text.toString().trim()
            val name = binding.etName.text.toString().trim()

            if (validateInput(username, password, confirmPassword, email, name)) {
                val user = User(username, password, email, name)
                if (dataManager.addUser(user)) {
                    Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(username: String, password: String, confirmPassword: String, email: String, name: String): Boolean {
        return when {
            username.isEmpty() -> {
                binding.etUsername.error = "Username is required"
                false
            }
            username.length < 3 -> {
                binding.etUsername.error = "Username must be at least 3 characters"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                false
            }
            password.length < 6 -> {
                binding.etPassword.error = "Password must be at least 6 characters"
                false
            }
            confirmPassword != password -> {
                binding.etConfirmPassword.error = "Passwords don't match"
                false
            }
            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Invalid email format"
                false
            }
            name.isEmpty() -> {
                binding.etName.error = "Name is required"
                false
            }
            else -> true
        }
    }
}
