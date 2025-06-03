package com.example.mycookbook.ui.screens

import android.content.res.Configuration
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.mycookbook.R
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.User
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenLayout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setContentView(R.layout.activity_register)
        } else {
            setContentView(R.layout.activity_register)
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent("register_screen_opened", null)

        firestore = FirebaseFirestore.getInstance()

        db = AppDatabase.getInstance(applicationContext)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            firebaseAnalytics.logEvent("register_button_clicked", Bundle().apply {
                putString("email_attempt", email)
            })

            if (firstName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                firebaseAnalytics.logEvent("register_validation_failed", null)
                Toast.makeText(this, getString(R.string.toast_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    firebaseAnalytics.logEvent("register_email_exists", Bundle().apply {
                        putString("email", email)
                    })
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, getString(R.string.toast_email_already_exists), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val newUser = User(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = password
                    )
                    val userId = db.userDao().insert(newUser)
                    newUser.id = userId.toInt()

                    uploadUserAndLogAnalytics(newUser, "email")

                    runOnUiThread {
                        firebaseAnalytics.logEvent("register_success", Bundle().apply {
                            putString("email", email)
                            putInt("user_id", newUser.id)
                        })
                        Toast.makeText(this@RegisterActivity, getString(R.string.toast_registration_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        tvBackToLogin.setOnClickListener {
            firebaseAnalytics.logEvent("back_to_login_from_register", null)
            finish()
        }
    }

    private suspend fun uploadUserAndLogAnalytics(user: User, loginType: String) {
        withContext(Dispatchers.IO) {
            try {
                val userData = hashMapOf(
                    "id" to user.id,
                    "firstName" to user.firstName,
                    "lastName" to user.lastName,
                    "email" to user.email,
                    "password" to user.password,
                    "loginType" to loginType
                )
                firestore.collection("users")
                    .document(user.id.toString())
                    .set(userData)
                    .addOnSuccessListener {
                        firebaseAnalytics.logEvent("user_data_uploaded_successfully", Bundle().apply {
                            putInt("user_id", user.id)
                        })
                    }
                    .addOnFailureListener { e ->
                        firebaseAnalytics.logEvent("user_data_upload_failed", Bundle().apply {
                            putInt("user_id", user.id)
                            putString("error", e.message)
                        })
                    }

                val bundle = Bundle().apply {
                    putString("registered_user_email", user.email)
                    putInt("user_id", user.id)
                }
                firebaseAnalytics.logEvent("user_registered", bundle)
            } catch (e: Exception) {
                firebaseAnalytics.logEvent("user_registration_exception", Bundle().apply {
                    putString("error", e.message)
                })
            }
        }
    }
}

