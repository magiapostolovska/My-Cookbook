package com.example.mycookbook

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager
import com.example.mycookbook.ui.screens.*
import java.util.Locale
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import android.content.res.Configuration

class MainActivity : ComponentActivity() {

    private var userId: Int = -1
    private var userEmail: String? = null
    private var loginType: String? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", "FCM token: $token")
            } else {
                Log.e("FCM Token", "Fetching FCM token failed", task.exception)
            }
        }
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        loginType = prefs.getString("login_type", null)
        userId = prefs.getInt("user_id", -1)
        userEmail = prefs.getString("user_email", null)

        Log.d("MainActivits", "onCreate SharedPreferences values: login_type=$loginType, user_id=$userId, user_email=$userEmail")

        if (loginType == null) {
            Log.d("MainActivits", "login_type is null, redirecting to LoginActivity")
            redirectToLogin()
            return
        } else if (loginType != "guest") {
            if (userId == -1) {
                Log.d("MainActivits", "user_id is invalid (-1), redirecting to LoginActivity")
                redirectToLogin()
                return
            }
            if (userEmail.isNullOrEmpty()) {
                Log.d("MainActivits", "user_email is null or empty, redirecting to LoginActivity")
                redirectToLogin()
                return
            }
        }

        setAppLocaleFromPreferences()

        val screenLayout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setContentView(R.layout.activity_main)
        } else {
            setContentView(R.layout.activity_main)
        }

        val ivUser = findViewById<ImageView>(R.id.ivUser)
        if (loginType == "guest") {
            ivUser.visibility = View.GONE
        } else {
            ivUser.visibility = View.VISIBLE
            ivUser.setOnClickListener {
                firebaseAnalytics.logEvent("open_user_profile", null)
                startActivity(Intent(this, UserProfileActivity::class.java))
            }
        }

        findViewById<ImageView>(R.id.ivSettings).setOnClickListener {
            firebaseAnalytics.logEvent("open_settings", null)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnAdd).setOnClickListener {
            firebaseAnalytics.logEvent("open_add_recipe", null)
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnBreakfast).setOnClickListener {
            openCategory(1)
        }

        findViewById<LinearLayout>(R.id.btnLunch).setOnClickListener {
            openCategory(2)
        }

        findViewById<LinearLayout>(R.id.btnDessert).setOnClickListener {
            openCategory(3)
        }

        findViewById<LinearLayout>(R.id.btnDrinks).setOnClickListener {
            openCategory(4)
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        loginType = prefs.getString("login_type", null)
        userId = prefs.getInt("user_id", -1)
        userEmail = prefs.getString("user_email", null)

        Log.d("MainActivits", "onResume SharedPreferences values: login_type=$loginType, user_id=$userId, user_email=$userEmail")

        if (loginType == null) {
            Log.d("MainActivits", "onResume: login_type is null, redirecting to LoginActivity")
            redirectToLogin()
            return
        } else if (loginType != "guest") {
            if (userId == -1) {
                Log.d("MainActivits", "onResume: user_id is invalid (-1), redirecting to LoginActivity")
                redirectToLogin()
                return
            }
            if (userEmail.isNullOrEmpty()) {
                Log.d("MainActivits", "onResume: user_email is null or empty, redirecting to LoginActivity")
                redirectToLogin()
                return
            }
        }

        if (prefs.getBoolean("language_changed", false)) {
            prefs.edit().putBoolean("language_changed", false).apply()
            recreate()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun openCategory(categoryId: Int) {
        val bundle = Bundle().apply {
            putInt("category_id", categoryId)
        }
        firebaseAnalytics.logEvent("open_category", bundle)

        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("category_id", categoryId)
        startActivity(intent)
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
