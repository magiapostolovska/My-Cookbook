package com.example.mycookbook.data.remote
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

suspend fun uploadCategoriesToFirestore() {
    val firestore = FirebaseFirestore.getInstance()
    val categories = listOf(
        Pair(1, "Breakfast"),
        Pair(2, "Lunch"),
        Pair(3, "Dessert"),
        Pair(4, "Drinks")
    )

    categories.forEach { (id, name) ->
        try {
            val data = mapOf(
                "id" to id,
                "name" to name,
                "userId" to 0
            )

            firestore.collection("categories")
                .document(id.toString())
                .set(data)
                .await()
            Log.d("FirestoreUpload", "Uploaded category $name with id $id")
        } catch (e: Exception) {
            Log.e("FirestoreUpload", "Failed to upload category $name: ${e.message}")
        }
    }
}
