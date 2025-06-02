package com.example.mycookbook.data.local.dao

import androidx.room.*
import com.example.mycookbook.data.local.entity.Recipe

@Dao
interface RecipeDao {
    @Update suspend fun update(recipe: Recipe)
    @Delete suspend fun delete(recipe: Recipe)

    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<Recipe>

    @Query("DELETE FROM recipes WHERE userId = :userId")
    suspend fun deleteRecipesByUserId(userId: Int)

    @Query("SELECT * FROM recipes WHERE categoryId = :categoryId AND guestId = :guestId")
    suspend fun getRecipesByCategoryIdAndGuestId(categoryId: Int, guestId: String): List<Recipe>
    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Query("DELETE FROM recipes WHERE guestId = :guestId")
    suspend fun deleteRecipesByGuestId(guestId: String)

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    fun getRecipeById(id: Int): Recipe?
}
