package com.example.mycookbook.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mycookbook.data.remote.model.FirestoreUser

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var firstName: String,
    var lastName: String,
    var email: String,
    var password: String
)


fun User.toFirestore(): FirestoreUser {
    return FirestoreUser(
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        password = this.password
    )
}

