package com.example.mycookbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.Category
import kotlinx.coroutines.launch

class CategoryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val dao = db.categoryDao()

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    fun loadAll() {
        viewModelScope.launch {
            val allCategories = dao.getAll()
            _categories.postValue(allCategories)
        }
    }

    fun add(category: Category) {
        viewModelScope.launch {
            dao.insert(category)
            loadAll()
        }
    }

    fun update(category: Category) {
        viewModelScope.launch {
            dao.update(category)
            loadAll()
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            dao.delete(category)
            loadAll()
        }
    }
}

