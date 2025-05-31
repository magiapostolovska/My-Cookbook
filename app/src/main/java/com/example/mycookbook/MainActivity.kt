package com.example.mycookbook

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager
import com.example.mycookbook.ui.screens.AddRecipeActivity
import com.example.mycookbook.ui.screens.CategoryActivity
import com.example.mycookbook.ui.screens.SettingsActivity
import com.example.mycookbook.ui.screens.UserProfileActivity
import java.util.Locale

class MainActivity : ComponentActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppLocaleFromPreferences()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageView>(R.id.ivSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageView>(R.id.ivUser).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnBreakfast).setOnClickListener {
            openCategory(categoryId = 1)
        }

        findViewById<LinearLayout>(R.id.btnLunch).setOnClickListener {
            openCategory(categoryId = 2)
        }

        findViewById<LinearLayout>(R.id.btnDessert).setOnClickListener {
            openCategory(categoryId = 3)
        }

        findViewById<LinearLayout>(R.id.btnDrinks).setOnClickListener {
            openCategory(categoryId = 4)
        }

    }


    private fun openCategory(categoryId: Int) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("category_id", categoryId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("language_changed", false)) {
            prefs.edit().putBoolean("language_changed", false).apply()
            recreate()
        }
    }

    private fun setAppLocaleFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("app_language", "en") ?: "en"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
