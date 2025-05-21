package com.example.mycookbook.data.local.dao

import androidx.room.*
import com.example.mycookbook.data.local.entity.User

@Dao
interface UserDao {
    @Insert suspend fun insert(user: User)
    @Update suspend fun update(user: User)
    @Delete suspend fun delete(user: User)

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<User>
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
}
