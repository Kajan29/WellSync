package com.example.wellsync.models

data class User(
    val username: String,
    val password: String,
    val email: String,
    val name: String = "",
    val profilePicture: String = ""
)
