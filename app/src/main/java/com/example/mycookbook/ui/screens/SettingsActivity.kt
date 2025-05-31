package com.example.mycookbook.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager
import com.example.mycookbook.MainActivity
import com.example.mycookbook.R
import java.util.*

class SettingsActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var languageChanged = false
    private var savedLang: String? = null
    private var returnTo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        returnTo = intent.getStringExtra("return_to")

        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val ivUserIcon = findViewById<ImageView>(R.id.ivUser)

        val languages = resources.getStringArray(R.array.language_options)
        savedLang = sharedPreferences.getString("app_language", "en")
        val savedPosition = if (savedLang == "mk") 1 else 0

        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item_selected,
            R.id.text1,
            languages
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = layoutInflater.inflate(android.R.layout.simple_spinner_item, parent, false)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)
                textView.setTextColor(android.graphics.Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = layoutInflater.inflate(R.layout.spinner_item_dropdown, parent, false)
                val textView = view.findViewById<TextView>(R.id.text1)
                val checkmark = view.findViewById<ImageView>(R.id.checkmark)

                textView.text = getItem(position)

                if (position == spinnerLanguage.selectedItemPosition) {
                    view.setBackgroundColor(android.graphics.Color.parseColor("#D3D3D3"))
                    textView.setTextColor(android.graphics.Color.BLACK)
                    checkmark.visibility = View.VISIBLE
                } else {
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    textView.setTextColor(android.graphics.Color.BLACK)
                    checkmark.visibility = View.GONE
                }

                return view
            }
        }

        spinnerLanguage.adapter = adapter
        spinnerLanguage.setSelection(savedPosition)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLang = if (position == 1) "mk" else "en"
                if (selectedLang != savedLang) {
                    sharedPreferences.edit()
                        .putString("app_language", selectedLang)
                        .putBoolean("language_changed", true)
                        .apply()
                    setLocale(selectedLang)
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        ivUserIcon.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("return_to", "SettingsActivity")
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        ivBackArrow.setOnClickListener {
            val intent = when (returnTo) {
                "CategoryActivity" -> Intent(this, CategoryActivity::class.java)
                "UserProfileActivity" -> Intent(this, UserProfileActivity::class.java)
                "AddRecipeActivity" -> Intent(this, AddRecipeActivity::class.java)
                "RecipeDetailActivity" -> Intent(this, RecipeDetailActivity::class.java)
                else -> Intent(this, MainActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (languageChanged) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
