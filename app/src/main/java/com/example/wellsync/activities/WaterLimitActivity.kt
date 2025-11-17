package com.example.wellsync.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wellsync.databinding.ActivityWaterLimitBinding
import com.example.wellsync.utils.DataManager

class WaterLimitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaterLimitBinding
    private lateinit var dataManager: DataManager
    private var currentUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaterLimitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)
        currentUser = dataManager.getCurrentUser()

        setupUI()
        loadCurrentLimit()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Water Limit Settings"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun loadCurrentLimit() {
        currentUser?.let { user ->
            val currentLimit = dataManager.getWaterGoal(user)
            binding.apply {
                tvCurrentLimit.text = "$currentLimit glasses"
                tvCurrentLimitLiters.text = "${String.format("%.1f", currentLimit * 0.25)}L"
                seekBarWaterLimit.progress = currentLimit - 1
                etCustomLimit.setText(currentLimit.toString())
            }
        }
    }

    private fun setupClickListeners() {
        binding.btn4Glasses.setOnClickListener { setWaterLimit(4) }
        binding.btn6Glasses.setOnClickListener { setWaterLimit(6) }
        binding.btn8Glasses.setOnClickListener { setWaterLimit(8) }
        binding.btn10Glasses.setOnClickListener { setWaterLimit(10) }
        binding.btn12Glasses.setOnClickListener { setWaterLimit(12) }

        binding.seekBarWaterLimit.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val glasses = progress + 1
                    updateLimitDisplay(glasses)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.btnSetCustom.setOnClickListener {
            val customLimit = binding.etCustomLimit.text.toString().toIntOrNull()
            if (customLimit != null && customLimit in 1..20) {
                setWaterLimit(customLimit)
            } else {
                Toast.makeText(this, "Please enter a valid number between 1 and 20", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSave.setOnClickListener {
            val newLimit = binding.seekBarWaterLimit.progress + 1
            saveWaterLimit(newLimit)
        }

        binding.btnReset.setOnClickListener {
            setWaterLimit(8)
        }
    }

    private fun setWaterLimit(glasses: Int) {
        binding.seekBarWaterLimit.progress = glasses - 1
        binding.etCustomLimit.setText(glasses.toString())
        updateLimitDisplay(glasses)
    }

    private fun updateLimitDisplay(glasses: Int) {
        binding.apply {
            tvCurrentLimit.text = "$glasses glasses"
            tvCurrentLimitLiters.text = "${String.format("%.1f", glasses * 0.25)}L"
            etCustomLimit.setText(glasses.toString())
        }
    }

    private fun saveWaterLimit(glasses: Int) {
        currentUser?.let { user ->
            dataManager.setWaterGoal(user, glasses)
            Toast.makeText(this, "Water limit updated to $glasses glasses", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
