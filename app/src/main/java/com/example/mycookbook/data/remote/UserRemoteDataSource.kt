package com.example.mycookbook.data.remote
import com.example.mycookbook.data.local.entity.User
import com.example.mycookbook.data.local.entity.toFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


fun uploadUser(user: User) {
    val firestoreUser = user.toFirestore()

    FirebaseFirestore.getInstance().collection("users")
        .document(user.email)
        .set(firestoreUser, SetOptions.merge())
        .addOnSuccessListener {
            println("User uploaded successfully!")
        }
        .addOnFailureListener { e ->
            println("Error uploading user: ${e.message}")
        }
}


