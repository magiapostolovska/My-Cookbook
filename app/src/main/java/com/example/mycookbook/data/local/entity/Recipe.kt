package com.example.mycookbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mycookbook.data.remote.model.FirestoreRecipe

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    val title: String,
    val ingredients: String,
    val instructions: String,
    val categoryId: Int,
    val userId: Int?,
    val isGuest: Boolean = false,
    val guestId: String? = null
)

fun Recipe.toFirestore(): FirestoreRecipe {
    return FirestoreRecipe(
        title = this.title,
        ingredients = this.ingredients,
        instructions = this.instructions,
        categoryId = this.categoryId,
        userId = this.userId,
        isGuest = this.isGuest,
        guestId = this.guestId
    )
}


