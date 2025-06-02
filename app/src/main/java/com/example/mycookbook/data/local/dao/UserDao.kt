package com.example.mycookbook.data.local.dao

import androidx.room.*
import com.example.mycookbook.data.local.entity.User

@Dao
interface UserDao {
    @Update suspend fun update(user: User)
    @Delete suspend fun delete(user: User)

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): User?

    @Insert
    suspend fun insert(user: User): Long

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

}

