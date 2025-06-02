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
    private val dao = AppDatabase.getInstance(app).userDao()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    fun loadUserById(userId: Int) {
        viewModelScope.launch {
            val foundUser = dao.getUserById(userId)
            _user.postValue(foundUser)
        }
    }

    fun add(user: User) {
        viewModelScope.launch {
            dao.insert(user)
        }
    }

    fun update(user: User) {
        viewModelScope.launch {
            dao.update(user)
            val refreshedUser = dao.getUserById(user.id)
            _user.postValue(refreshedUser)
        }
    }

    fun delete(user: User) {
        viewModelScope.launch {
            dao.delete(user)
            _user.postValue(null)
        }
    }
}
