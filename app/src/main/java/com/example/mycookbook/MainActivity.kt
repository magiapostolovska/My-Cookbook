package com.example.mycookbook

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager
import android.widget.ImageView
import com.example.mycookbook.ui.screens.SettingsActivity
import com.example.mycookbook.ui.screens.UserProfileActivity
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppLocaleFromPreferences()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val ivUser = findViewById<ImageView>(R.id.ivUser)
        ivUser.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val languageChanged = prefs.getBoolean("language_changed", false)
        if (languageChanged) {
            prefs.edit().putBoolean("language_changed", false).apply()
            recreate()
        }
    }

    private fun setAppLocaleFromPreferences() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val language = sharedPreferences.getString("app_language", "en") ?: "en"

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
