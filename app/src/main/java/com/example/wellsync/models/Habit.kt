package com.example.wellsync.models

data class Habit(
    val name: String,
    var done: Boolean = false,
    val id: String = java.util.UUID.randomUUID().toString()
)
