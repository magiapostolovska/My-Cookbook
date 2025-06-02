package com.example.mycookbook.ui.screens

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycookbook.MainActivity
import com.example.mycookbook.R
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.Recipe
import com.example.mycookbook.ui.adapter.RecipeAdapter
import com.example.mycookbook.utils.SessionManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CategoryActivity : ComponentActivity() {

    private lateinit var guestId: String
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val firestore = FirebaseFirestore.getInstance()

    private var categoryId: Int = -1
    private var loginType: String? = null
    private var userId: Int = -1
    private lateinit var noteListView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        categoryId = intent.getIntExtra("category_id", -1)
        if (categoryId == -1) {
            firebaseAnalytics.logEvent("invalid_category_id", null)
            finish()
            return
        }

        val layoutResId = when (categoryId) {
            1 -> R.layout.activity_category
            2 -> R.layout.activity_category2
            3 -> R.layout.activity_category3
            4 -> R.layout.activity_category4
            else -> R.layout.activity_category
        }
        setContentView(layoutResId)

        firebaseAnalytics.logEvent("open_category_screen", Bundle().apply {
            putInt("category_id", categoryId)
        })

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        loginType = prefs.getString("login_type", null)
        userId = prefs.getInt("user_id", -1)

        guestId = SessionManager(this).getGuestId()

        val ivUser = findViewById<ImageView>(R.id.ivUser)
        ivUser.visibility = if (loginType == "guest" || loginType == null) View.GONE else View.VISIBLE

        noteListView = findViewById(R.id.noteRecyclerView)
        noteListView.layoutManager = LinearLayoutManager(this)
        val spaceHeight = resources.getDimensionPixelSize(R.dimen.item_vertical_spacing)
        noteListView.addItemDecoration(VerticalSpaceItemDecoration(spaceHeight))

        val ivSettings = findViewById<ImageView>(R.id.ivSettings)

        ivUser.setOnClickListener {
            firebaseAnalytics.logEvent("click_user_icon", Bundle().apply {
                putInt("category_id", categoryId)
            })
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("return_to", "CategoryActivity")
            intent.putExtra("category_id", categoryId)
            startActivity(intent)
        }

        ivSettings.setOnClickListener {
            firebaseAnalytics.logEvent("click_settings_icon", Bundle().apply {
                putInt("category_id", categoryId)
            })
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("return_to", "CategoryActivity")
            intent.putExtra("category_id", categoryId)
            startActivity(intent)
        }

        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        ivBackArrow.setOnClickListener {
            firebaseAnalytics.logEvent("click_back_arrow", Bundle().apply {
                putInt("category_id", categoryId)
            })
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        loadRecipes()
    }

    override fun onResume() {
        super.onResume()
        loadRecipes()
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            val recipes = if (loginType == "guest" || loginType == null) {
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance(this@CategoryActivity).recipeDao()
                    dao.getRecipesByCategoryIdAndGuestId(categoryId, guestId)
                }
            } else {
                fetchRecipesFromFirestore(categoryId, userId)
            }

            Log.d("CategoryActivity", "Loaded recipes count: ${recipes.size}")
            recipes.forEach {
                Log.d(
                    "CategoryActivity",
                    "Recipe: id=${it.id}, title=${it.title}, userId=${it.userId}, guestId=${it.guestId}, categoryId=${it.categoryId}"
                )
            }

            noteListView.adapter = RecipeAdapter(
                recipes,
                onArrowClick = { recipe ->
                    firebaseAnalytics.logEvent("click_recipe_card", Bundle().apply {
                        putString("recipe_title", recipe.title)
                        putInt("category_id", categoryId)
                    })
                    val intent = Intent(this@CategoryActivity, RecipeDetailActivity::class.java)
                    intent.putExtra("recipe_title", recipe.title)
                    intent.putExtra("recipe_ingredients", recipe.ingredients)
                    intent.putExtra("recipe_instructions", recipe.instructions)
                    intent.putExtra("recipe_category_id", categoryId)
                    intent.putExtra("recipe_id", recipe.id)
                    startActivity(intent)
                }
            )
        }
    }

    private suspend fun fetchRecipesFromFirestore(categoryId: Int, userId: Int): List<Recipe> {
        return withContext(Dispatchers.IO) {
            if (userId == -1) {
                Log.w("CategoryActivity", "Invalid user ID")
                firebaseAnalytics.logEvent("fetch_recipes_invalid_user", null)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CategoryActivity, getString(R.string.invalid_user_id), Toast.LENGTH_SHORT).show()
                }
                return@withContext emptyList()
            }

            try {
                val snapshot = firestore.collection("recipes")
                    .whereEqualTo("categoryId", categoryId)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    try {
                        Recipe(
                            id = doc.id.toIntOrNull() ?: 0,
                            title = doc.getString("title") ?: "",
                            ingredients = doc.getString("ingredients") ?: "",
                            instructions = doc.getString("instructions") ?: "",
                            categoryId = (doc.getLong("categoryId") ?: 0L).toInt(),
                            userId = (doc.getLong("userId") ?: 0L).toInt(),
                            isGuest = doc.getBoolean("isGuest") ?: false,
                            guestId = doc.getString("guestId")
                        )
                    } catch (e: Exception) {
                        Log.e("CategoryActivity", "Error parsing recipe ${doc.id}: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("CategoryActivity", "Failed to fetch recipes: ${e.message}")
                firebaseAnalytics.logEvent("fetch_recipes_failed", Bundle().apply {
                    putString("error_message", e.message)
                })
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CategoryActivity, getString(R.string.recipes_load_failed), Toast.LENGTH_SHORT).show()
                }
                emptyList()
            }
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}

