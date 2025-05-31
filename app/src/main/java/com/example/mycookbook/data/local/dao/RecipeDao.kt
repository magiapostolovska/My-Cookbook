package com.example.mycookbook.data.local.dao

import androidx.room.*
import com.example.mycookbook.data.local.entity.Recipe

@Dao
interface RecipeDao {
    @Insert suspend fun insert(recipe: Recipe)
    @Update suspend fun update(recipe: Recipe)
    @Delete suspend fun delete(recipe: Recipe)

    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE categoryId = :categoryId")
    suspend fun getRecipesByCategoryId(categoryId: Int): List<Recipe>
}
