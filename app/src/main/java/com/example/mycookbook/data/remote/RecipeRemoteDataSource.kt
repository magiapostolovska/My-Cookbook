package com.example.mycookbook.data.remote

import com.example.mycookbook.data.local.entity.toFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.mycookbook.data.local.entity.Recipe as LocalRecipe

fun uploadRecipeToFirestore(recipe: LocalRecipe) {
    val firestore = FirebaseFirestore.getInstance()
    val firestoreRecipe = recipe.toFirestore()

    val recipeMap = hashMapOf(
        "id" to recipe.id,
        "title" to firestoreRecipe.title,
        "ingredients" to firestoreRecipe.ingredients,
        "instructions" to firestoreRecipe.instructions,
        "categoryId" to firestoreRecipe.categoryId,
        "userId" to firestoreRecipe.userId,
        "isGuest" to firestoreRecipe.isGuest,
        "guestId" to firestoreRecipe.guestId,
        "createdAt" to firestoreRecipe.createdAt
    )

    val docRef = firestore.collection("recipes").document(recipe.id.toString())

    docRef.set(recipeMap, SetOptions.merge())
        .addOnSuccessListener {
            println("Recipe uploaded successfully with id: ${recipe.id}!")
        }
        .addOnFailureListener { e ->
            println("Error uploading recipe: ${e.message}")
        }
}
