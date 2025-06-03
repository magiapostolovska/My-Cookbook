package com.example.mycookbook.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.mycookbook.R
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.Category
import com.example.mycookbook.data.local.entity.Recipe
import com.example.mycookbook.data.remote.uploadRecipeToFirestore
import com.example.mycookbook.utils.SessionManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class AddRecipeActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSaveRecipe: Button
    private lateinit var ivBackArrow: ImageView
    private lateinit var ivUser: ImageView
    private lateinit var ivSettings: ImageView
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val auth = FirebaseAuth.getInstance()
    private var currentLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenLayout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setContentView(R.layout.activity_add_recipe)
        } else {
            setContentView(R.layout.activity_add_recipe)
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent("open_add_recipe_screen", null)

        db = AppDatabase.getInstance(this)

        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnSaveRecipe = findViewById(R.id.btnAddRecipe)
        ivBackArrow = findViewById(R.id.ivBackArrow)
        ivUser = findViewById(R.id.ivUser)
        ivSettings = findViewById(R.id.ivSettings)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        currentLanguage = sharedPreferences.getString("app_language", "en")

        val categoryTranslationMapMk = mapOf(
            "Breakfast" to "Појадок",
            "Lunch" to "Ручек",
            "Dessert" to "Десерт",
            "Drinks" to "Пијалоци"
        )
        val categoryTranslationMapEn = mapOf(
            "Breakfast" to "Breakfast",
            "Lunch" to "Lunch",
            "Dessert" to "Dessert",
            "Drinks" to "Drinks"
        )

        val isGuest = sharedPreferences.getString("login_type", "") == "guest"

        if (isGuest) {
            ivUser.visibility = View.GONE
        }

        ivSettings.setOnClickListener {
            firebaseAnalytics.logEvent("open_settings_from_add_recipe", null)
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("return_to", "AddRecipeActivity")
            startActivity(intent)
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ensureDefaultCategoriesExist(db)
            }

            val categories = withContext(Dispatchers.IO) {
                db.categoryDao().getAll()
            }

            val categoryNames = if (currentLanguage == "mk") {
                categories.map { categoryTranslationMapMk[it.name] ?: it.name }
            } else {
                categories.map { categoryTranslationMapEn[it.name] ?: it.name }
            }

            val adapter = object : ArrayAdapter<String>(
                this@AddRecipeActivity,
                R.layout.spinner_item_selected,
                R.id.text1,
                categoryNames
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = layoutInflater.inflate(R.layout.spinner_item_selected, parent, false)
                    val textView = view.findViewById<TextView>(R.id.text1)
                    textView.text = getItem(position)
                    textView.setTextColor(Color.BLACK)
                    view.background = null
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = layoutInflater.inflate(R.layout.spinner_item_dropdown, parent, false)
                    val textView = view.findViewById<TextView>(R.id.text1)
                    val checkmark = view.findViewById<ImageView>(R.id.checkmark)
                    textView.text = getItem(position)
                    if (position == spinnerCategory.selectedItemPosition) {
                        view.setBackgroundColor(Color.parseColor("#D3D3D3"))
                        textView.setTextColor(Color.BLACK)
                        checkmark.visibility = View.VISIBLE
                    } else {
                        view.setBackgroundColor(Color.WHITE)
                        textView.setTextColor(Color.BLACK)
                        checkmark.visibility = View.GONE
                    }
                    return view
                }
            }
            spinnerCategory.adapter = adapter
        }

        ivBackArrow.setOnClickListener {
            firebaseAnalytics.logEvent("click_back_arrow", null)
            finish()
        }

        ivUser.setOnClickListener {
            firebaseAnalytics.logEvent("open_user_profile_from_add_recipe", null)
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("return_to", "AddRecipeActivity")
            startActivity(intent)
        }

        btnSaveRecipe.setOnClickListener {
            firebaseAnalytics.logEvent("click_save_recipe_button", null)

            val recipeName = findViewById<EditText>(R.id.etRecipeName).text.toString().trim()
            val ingredients = findViewById<EditText>(R.id.etIngredients).text.toString().trim()
            val instructions = findViewById<EditText>(R.id.etInstructions).text.toString().trim()
            val selectedPos = spinnerCategory.selectedItemPosition

            if (recipeName.isEmpty() || ingredients.isEmpty() || instructions.isEmpty() || selectedPos == AdapterView.INVALID_POSITION) {
                firebaseAnalytics.logEvent("save_recipe_failed_empty_fields", null)
                Toast.makeText(this, getString(R.string.toast_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val categories = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                val selectedCategory = categories[selectedPos]
                val selectedCategoryId = selectedCategory.id

                val prefs = PreferenceManager.getDefaultSharedPreferences(this@AddRecipeActivity)
                val loggedInUserId = prefs.getInt("user_id", -1)
                val isGuest = prefs.getString("login_type", "") == "guest"
                val guestId = SessionManager(this@AddRecipeActivity).getGuestId()

                val newRecipe = Recipe(
                    title = recipeName,
                    ingredients = ingredients,
                    instructions = instructions,
                    categoryId = selectedCategoryId,
                    userId = if (isGuest) null else loggedInUserId,
                    isGuest = isGuest,
                    guestId = if (isGuest) guestId else null
                )

                val generatedId = withContext(Dispatchers.IO) {
                    db.recipeDao().insert(newRecipe)
                }.toInt()

                val recipeWithId = newRecipe.copy(id = generatedId)

                if (!isGuest) {
                    uploadRecipeToFirestore(recipeWithId)
                }

                firebaseAnalytics.logEvent("save_recipe_success", Bundle().apply {
                    putString("recipe_title", recipeName)
                    putInt("category_id", selectedCategoryId)
                    putBoolean("is_guest", isGuest)
                })

                Toast.makeText(this@AddRecipeActivity, getString(R.string.toast_recipe_saved), Toast.LENGTH_SHORT).show()

                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val newLanguage = sharedPreferences.getString("app_language", "en")
        if (newLanguage != currentLanguage) {
            currentLanguage = newLanguage
            recreate()
        }
    }

    private suspend fun ensureDefaultCategoriesExist(db: AppDatabase) {
        val categories = db.categoryDao().getAll()
        if (categories.isEmpty()) {
            val defaultCategories = listOf(
                Category(name = "Breakfast", userId = -1),
                Category(name = "Lunch", userId = -1),
                Category(name = "Dessert", userId = -1),
                Category(name = "Drinks", userId = -1)
            )
            defaultCategories.forEach {
                db.categoryDao().insert(it)
            }
        }
    }
}

