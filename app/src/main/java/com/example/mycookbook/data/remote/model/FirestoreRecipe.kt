package com.example.mycookbook.data.remote.model

data class FirestoreRecipe(
    val title: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val categoryId: Int = 0,
    val userId: Int? = null,
    val isGuest: Boolean = false,
    val guestId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
