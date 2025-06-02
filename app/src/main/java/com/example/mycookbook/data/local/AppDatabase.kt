package com.example.mycookbook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mycookbook.data.local.dao.CategoryDao
import com.example.mycookbook.data.local.dao.RecipeDao
import com.example.mycookbook.data.local.dao.UserDao
import com.example.mycookbook.data.local.entity.Category
import com.example.mycookbook.data.local.entity.Recipe
import com.example.mycookbook.data.local.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Category::class, Recipe::class], version = 4)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cookbook.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).categoryDao().apply {
                                    insert(Category(name = "Breakfast", userId = 0))
                                    insert(Category(name = "Lunch", userId = 0))
                                    insert(Category(name = "Dessert", userId = 0))
                                    insert(Category(name = "Drinks", userId = 0))
                                }
                            }
                        }
                    })

                    .build()
                INSTANCE = instance
                instance
            }

        }
    }
}

