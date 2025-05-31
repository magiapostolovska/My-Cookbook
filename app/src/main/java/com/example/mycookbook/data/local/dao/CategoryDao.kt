package com.example.mycookbook.data.local.dao

import androidx.room.*
import com.example.mycookbook.data.local.entity.Category

@Dao
interface CategoryDao {
    @Insert suspend fun insert(category: Category)
    @Update suspend fun update(category: Category)
    @Delete suspend fun delete(category: Category)

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<Category>


    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

}


