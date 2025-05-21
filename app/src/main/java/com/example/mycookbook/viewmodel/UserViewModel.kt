package com.example.mycookbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.User
import kotlinx.coroutines.launch

class UserViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val dao = db.userDao()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    fun loadAll() {
        viewModelScope.launch {
            val allUsers = dao.getAll()
            _users.postValue(allUsers)
        }
    }

    fun add(user: User) {
        viewModelScope.launch {
            dao.insert(user)
            loadAll()
        }
    }

    fun update(user: User) {
        viewModelScope.launch {
            dao.update(user)
            loadAll()
        }
    }

    fun delete(user: User) {
        viewModelScope.launch {
            dao.delete(user)
            loadAll()
        }
    }
}
