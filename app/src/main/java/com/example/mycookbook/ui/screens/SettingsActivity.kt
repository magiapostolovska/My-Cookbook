package com.example.mycookbook.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.mycookbook.*
import com.example.mycookbook.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SettingsActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private var languageChanged = false
    private var savedLang: String? = null
    private var returnTo: String? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenLayout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setContentView(R.layout.activity_settings)
        } else {
            setContentView(R.layout.activity_settings)
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent("settings_screen_opened", null)


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        returnTo = intent.getStringExtra("return_to")

        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        val ivBackArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val ivUserIcon = findViewById<ImageView>(R.id.ivUser)

        val languages = resources.getStringArray(R.array.language_options)
        savedLang = sharedPreferences.getString("app_language", "en")
        val savedPosition = if (savedLang == "mk") 1 else 0
        val loginType = sharedPreferences.getString("login_type", null)
        if (loginType == "guest") {
            ivUserIcon.visibility = View.GONE
        } else {
            ivUserIcon.visibility = View.VISIBLE
            ivUserIcon.setOnClickListener {
                firebaseAnalytics.logEvent("settings_user_icon_clicked", null)
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("return_to", "SettingsActivity")
                startActivity(intent)
            }
        }

        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item_selected,
            R.id.text1,
            languages
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view =
                    layoutInflater.inflate(android.R.layout.simple_spinner_item, parent, false)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)
                textView.setTextColor(android.graphics.Color.BLACK)
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = layoutInflater.inflate(R.layout.spinner_item_dropdown, parent, false)
                val textView = view.findViewById<TextView>(R.id.text1)
                val checkmark = view.findViewById<ImageView>(R.id.checkmark)
                textView.text = getItem(position)
                if (position == spinnerLanguage.selectedItemPosition) {
                    view.setBackgroundColor(android.graphics.Color.parseColor("#D3D3D3"))
                    textView.setTextColor(android.graphics.Color.BLACK)
                    checkmark.visibility = View.VISIBLE
                } else {
                    view.setBackgroundColor(android.graphics.Color.WHITE)
                    textView.setTextColor(android.graphics.Color.BLACK)
                    checkmark.visibility = View.GONE
                }
                return view
            }
        }

        spinnerLanguage.adapter = adapter
        spinnerLanguage.setSelection(savedPosition)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLang = if (position == 1) "mk" else "en"
                if (selectedLang != savedLang) {
                    sharedPreferences.edit()
                        .putString("app_language", selectedLang)
                        .putBoolean("language_changed", true)
                        .apply()
                    setLocale(selectedLang)
                    firebaseAnalytics.logEvent("language_changed", Bundle().apply {
                        putString("language", selectedLang)
                    })
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.toast_language_changed),
                        Toast.LENGTH_SHORT
                    ).show()
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogout.setOnClickListener {
            firebaseAnalytics.logEvent("settings_logout_clicked", null)
            val loginType = sharedPreferences.getString("login_type", null)
            val guestId = sharedPreferences.getString("guest_id", null)
            val db = AppDatabase.getInstance(this)

            lifecycleScope.launch {
                if (loginType == "guest" && !guestId.isNullOrEmpty()) {
                    withContext(Dispatchers.IO) {
                        db.recipeDao().deleteRecipesByGuestId(guestId)
                    }
                }

                googleSignInClient.signOut().addOnCompleteListener {
                    sharedPreferences.edit()
                        .remove("user_id")
                        .remove("auth_token")
                        .remove("login_type")
                        .remove("guest_id")
                        .apply()

                    firebaseAnalytics.logEvent("user_logged_out", null)

                    val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        ivBackArrow.setOnClickListener {
            firebaseAnalytics.logEvent("settings_back_arrow_pressed", Bundle().apply {
                putString("return_to", returnTo)
            })
            val intent = when (returnTo) {
                "UserProfileActivity" -> Intent(this, UserProfileActivity::class.java)
                "AddRecipeActivity" -> Intent(this, AddRecipeActivity::class.java)
                "CategoryActivity" -> Intent(this, CategoryActivity::class.java).apply {
                    putExtra("category_id", intent.getIntExtra("category_id", -1))
                }
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
            firebaseAnalytics.logEvent("language_change_committed", Bundle().apply {
                putString("new_language", sharedPreferences.getString("app_language", "unknown"))
            })
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
