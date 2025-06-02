package com.example.mycookbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.Recipe
import kotlinx.coroutines.launch

class RecipeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val dao = db.recipeDao()

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    fun loadAll() {
        viewModelScope.launch {
            val allRecipes = dao.getAll()
            _recipes.postValue(allRecipes)
        }
    }

    fun add(recipe: Recipe) {
        viewModelScope.launch {
            dao.insert(recipe)
            loadAll()
        }
    }

    fun update(recipe: Recipe) {
        viewModelScope.launch {
            dao.update(recipe)
            loadAll()
        }
    }

    fun delete(recipe: Recipe) {
        viewModelScope.launch {
            dao.delete(recipe)
            loadAll()
        }
    }
    fun loadGuestRecipesByCategory(guestId: String, categoryId: Int) {
        viewModelScope.launch {
            val guestRecipes = dao.getRecipesByCategoryIdAndGuestId(categoryId, guestId)
            _recipes.postValue(guestRecipes)
        }
    }
}
