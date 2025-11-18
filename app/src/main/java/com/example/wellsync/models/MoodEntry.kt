package com.example.wellsync.models

data class MoodEntry(
    val date: String,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)
