package com.example.mycookbook.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.mycookbook.R
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.Recipe
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class RecipeDetailActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var db: FirebaseFirestore

    private var currentRecipe: Recipe? = null
    private var recipeId: Int = 0
    private var categoryId: Int = 0
    private var currentLanguage: String = "en"

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val language = prefs.getString("app_language", "en") ?: "en"
        val localeUpdatedContext = updateLocale(newBase, language)
        super.attachBaseContext(localeUpdatedContext)
    }

    private fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        db = FirebaseFirestore.getInstance()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        currentLanguage = prefs.getString("app_language", "en") ?: "en"

        recipeId = intent.getIntExtra("recipe_id", 0)
        categoryId = intent.getIntExtra("recipe_category_id", 0)

        if (recipeId == 0) {
            firebaseAnalytics.logEvent("recipe_id_missing", null)
            Toast.makeText(this, getString(R.string.recipe_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val layoutResId = when (categoryId) {
            1 -> R.layout.activity_recipe_detail
            2 -> R.layout.activity_recipe_detail2
            3 -> R.layout.activity_recipe_detail3
            4 -> R.layout.activity_recipe_detail4
            else -> R.layout.activity_recipe_detail
        }
        setContentView(layoutResId)

        val ivDelete = findViewById<ImageView>(R.id.ivDelete)
        ivDelete?.isEnabled = false

        val loginType = prefs.getString("login_type", null)
        val isGuest = loginType == "guest" || loginType == "anonymous"

        val ivUser = findViewById<ImageView>(R.id.ivUser)
        ivUser.visibility = if (isGuest) android.view.View.GONE else android.view.View.VISIBLE
        ivUser.setOnClickListener {
            firebaseAnalytics.logEvent("open_user_profile_from_recipe_detail", null)
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("return_to", "RecipeDetailActivity")
            intent.putExtra("category_id", categoryId)
            startActivity(intent)
        }

        lifecycleScope.launch {
            try {
                val recipe = if (isGuest) {
                    withContext(Dispatchers.IO) {
                        val dao = AppDatabase.getInstance(applicationContext).recipeDao()
                        dao.getRecipeById(recipeId)
                    }
                } else {
                    val docSnapshot = withContext(Dispatchers.IO) {
                        db.collection("recipes").document(recipeId.toString()).get().await()
                    }

                    if (!docSnapshot.exists()) {
                        firebaseAnalytics.logEvent("recipe_not_found_firestore", Bundle().apply {
                            putInt("recipe_id", recipeId)
                        })
                        Toast.makeText(this@RecipeDetailActivity, getString(R.string.recipe_not_found), Toast.LENGTH_SHORT).show()
                        finish()
                        return@launch
                    }

                    Recipe(
                        id = recipeId,
                        title = docSnapshot.getString("title") ?: "No Title",
                        ingredients = docSnapshot.getString("ingredients") ?: "",
                        instructions = docSnapshot.getString("instructions") ?: "",
                        categoryId = (docSnapshot.getLong("categoryId") ?: 0L).toInt(),
                        userId = (docSnapshot.getLong("userId") ?: 0L).toInt(),
                        isGuest = docSnapshot.getBoolean("isGuest") ?: false,
                        guestId = docSnapshot.getString("guestId")
                    )
                }

                if (recipe == null) {
                    firebaseAnalytics.logEvent("recipe_not_found_local", Bundle().apply {
                        putInt("recipe_id", recipeId)
                    })
                    Toast.makeText(this@RecipeDetailActivity, getString(R.string.recipe_not_found), Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                currentRecipe = recipe

                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.tvTitle).text = recipe.title
                    findViewById<TextView>(R.id.tvIngredients).text = recipe.ingredients
                    findViewById<TextView>(R.id.tvInstructions).text = recipe.instructions
                    ivDelete?.isEnabled = true
                }
            } catch (e: Exception) {
                firebaseAnalytics.logEvent("recipe_load_failed", Bundle().apply {
                    putString("error", e.message)
                    putInt("recipe_id", recipeId)
                })
                Toast.makeText(this@RecipeDetailActivity, getString(R.string.recipe_failed_to_load), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        firebaseAnalytics.logEvent("open_recipe_detail", Bundle().apply {
            putInt("category_id", categoryId)
            putInt("recipe_id", recipeId)
        })

        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            firebaseAnalytics.logEvent("click_back_arrow_from_recipe_detail", null)
            finish()
        }

        findViewById<ImageView>(R.id.ivSettings).setOnClickListener {
            firebaseAnalytics.logEvent("open_settings_from_recipe_detail", null)
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("return_to", "RecipeDetailActivity")
            intent.putExtra("category_id", categoryId)
            startActivity(intent)
        }

        ivDelete?.setOnClickListener {
            firebaseAnalytics.logEvent("click_delete_recipe", Bundle().apply {
                putInt("recipe_id", recipeId)
            })
            showDeleteConfirmationDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("app_language", "en") ?: "en"
        if (language != currentLanguage) {
            currentLanguage = language
            recreate()
        }
    }

    private fun showDeleteConfirmationDialog() {
        firebaseAnalytics.logEvent("show_delete_confirmation_dialog", Bundle().apply {
            putInt("recipe_id", recipeId)
        })
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_recipe_title))
            .setMessage(getString(R.string.delete_recipe_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteRecipe() }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                firebaseAnalytics.logEvent("cancel_delete_recipe", Bundle().apply {
                    putInt("recipe_id", recipeId)
                })
            }
            .show()
    }

    private fun deleteRecipe() {
        currentRecipe?.let { recipe ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val dao = AppDatabase.getInstance(applicationContext).recipeDao()
                    dao.delete(recipe)

                    if (!recipe.isGuest) {
                        db.collection("recipes").document(recipe.id.toString()).delete().await()
                    }

                    withContext(Dispatchers.Main) {
                        firebaseAnalytics.logEvent("delete_recipe", Bundle().apply {
                            putInt("category_id", recipe.categoryId)
                            putString("recipe_title", recipe.title)
                            putInt("recipe_id", recipe.id)
                        })
                        Toast.makeText(this@RecipeDetailActivity, getString(R.string.recipe_deleted_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    firebaseAnalytics.logEvent("delete_recipe_failed", Bundle().apply {
                        putInt("recipe_id", recipe.id)
                        putString("error", e.message)
                    })
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RecipeDetailActivity, getString(R.string.recipe_delete_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            firebaseAnalytics.logEvent("delete_recipe_attempt_before_loaded", Bundle().apply {
                putInt("recipe_id", recipeId)
            })
            Toast.makeText(this, getString(R.string.recipe_not_loaded_yet), Toast.LENGTH_SHORT).show()
        }
    }
}

