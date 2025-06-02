package com.example.mycookbook.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.mycookbook.*
import com.example.mycookbook.data.local.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class UserProfileActivity : ComponentActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var ivEdit: ImageView
    private lateinit var btnSave: Button
    private lateinit var ivSettings: ImageView
    private lateinit var btnDeleteAccount: ImageView

    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val firestore = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null

    private var isEditing = false
    private var returnTo: String? = null
    private var currentUserId: String? = null

    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent("user_profile_screen_opened", null)

        returnTo = intent.getStringExtra("return_to")
        appDatabase = AppDatabase.getInstance(this)

        ivBackArrow = findViewById(R.id.ivBackArrow)
        ivEdit = findViewById(R.id.ivEdit)
        btnSave = findViewById(R.id.btnSave)
        ivSettings = findViewById(R.id.ivSettings)
        btnDeleteAccount = findViewById(R.id.ivDelete)

        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val userIdInt = prefs.getInt("user_id", -1)

        if (userIdInt != -1) {
            val userId = userIdInt.toString()
            currentUserId = userId
            firebaseAnalytics.logEvent("user_profile_found", Bundle().apply {
                putString("user_id", userId)
            })
            loadUserProfile(userId)
        } else {
            firebaseAnalytics.logEvent("user_profile_missing", null)
            finish()
            return
        }

        ivBackArrow.setOnClickListener {
            firebaseAnalytics.logEvent("user_profile_back_pressed", Bundle().apply {
                putBoolean("editing_mode", isEditing)
            })
            if (isEditing) {
                currentUserId?.let { loadUserProfile(it) }
                toggleEditMode(false)
                firebaseAnalytics.logEvent("user_profile_edit_cancelled", null)
                Toast.makeText(this, getString(R.string.edit_cancelled), Toast.LENGTH_SHORT).show()
            } else {
                navigateBack()
            }
        }

        ivEdit.setOnClickListener {
            firebaseAnalytics.logEvent("user_profile_edit_button_clicked", null)
            toggleEditMode(true)
            firebaseAnalytics.logEvent("user_profile_edit_started", null)
        }

        ivSettings.setOnClickListener {
            firebaseAnalytics.logEvent("user_profile_settings_clicked", null)
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("return_to", "UserProfileActivity")
            startActivity(intent)
        }

        btnSave.setOnClickListener {
            firebaseAnalytics.logEvent("user_profile_save_clicked", null)
            saveUserData()
        }

        btnDeleteAccount.setOnClickListener {
            firebaseAnalytics.logEvent("user_profile_delete_clicked", null)
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_account_title))
            .setMessage(getString(R.string.delete_account_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadUserProfile(userId: String) {
        userListener?.remove()
        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, getString(R.string.load_profile_failed), Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val firstName = snapshot.getString("firstName") ?: ""
                    val lastName = snapshot.getString("lastName") ?: ""
                    val email = snapshot.getString("email") ?: ""

                    runOnUiThread {
                        populateUserInfo(firstName, lastName, email)
                        toggleEditMode(false)
                        firebaseAnalytics.logEvent("user_profile_loaded", Bundle().apply {
                            putString("user_id", userId)
                        })
                    }
                }
            }
    }

    private fun populateUserInfo(firstName: String, lastName: String, email: String) {
        tvFirstName.text = firstName
        tvLastName.text = lastName
        tvEmail.text = email

        etFirstName.setText(firstName)
        etLastName.setText(lastName)
        etEmail.setText(email)
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable

        tvFirstName.visibility = if (enable) View.GONE else View.VISIBLE
        tvLastName.visibility = if (enable) View.GONE else View.VISIBLE
        tvEmail.visibility = if (enable) View.GONE else View.VISIBLE

        etFirstName.visibility = if (enable) View.VISIBLE else View.GONE
        etLastName.visibility = if (enable) View.VISIBLE else View.GONE
        etEmail.visibility = if (enable) View.VISIBLE else View.GONE

        btnSave.visibility = if (enable) View.VISIBLE else View.GONE
        ivEdit.visibility = if (enable) View.GONE else View.VISIBLE

        btnDeleteAccount.visibility = if (enable) View.GONE else View.VISIBLE
    }

    private fun saveUserData() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            firebaseAnalytics.logEvent("user_profile_save_failed", Bundle().apply {
                putString("reason", "empty_fields")
            })
            return
        }

        val userId = currentUserId ?: return

        val userMap = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email
        )

        firestore.collection("users").document(userId)
            .update(userMap)
            .addOnSuccessListener {
                toggleEditMode(false)
                Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()

                firebaseAnalytics.logEvent("user_profile_updated", Bundle().apply {
                    putString("user_id", userId)
                    putString("first_name", firstName)
                    putString("last_name", lastName)
                    putString("email", email)
                })
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.profile_update_failed), Toast.LENGTH_LONG).show()
                firebaseAnalytics.logEvent("user_profile_update_failed", Bundle().apply {
                    putString("user_id", userId)
                })
            }
    }

    private fun deleteUserAccount() {
        val userIdStr = currentUserId ?: return
        val userId = userIdStr.toIntOrNull()
        if (userId == null) {
            firebaseAnalytics.logEvent("user_account_delete_failed", Bundle().apply {
                putString("reason", "invalid_user_id")
            })
            Toast.makeText(this, getString(R.string.invalid_user_id), Toast.LENGTH_LONG).show()
            return
        }

        firestore.collection("users").document(userIdStr)
            .delete()
            .addOnSuccessListener {
                firebaseAnalytics.logEvent("user_firestore_deleted", null)

                lifecycleScope.launch {
                    appDatabase.recipeDao().deleteRecipesByUserId(userId)
                    appDatabase.userDao().deleteUserById(userId)
                    firebaseAnalytics.logEvent("user_local_data_deleted", null)
                }

                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                prefs.edit()
                    .remove("user_id")
                    .remove("guest_id")
                    .apply()

                firebaseAnalytics.logEvent("user_account_deleted", Bundle().apply {
                    putString("user_id", userIdStr)
                })

                Toast.makeText(this, getString(R.string.account_deleted), Toast.LENGTH_LONG).show()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_LONG).show()
                firebaseAnalytics.logEvent("user_firestore_delete_failed", null)
            }
    }

    private fun navigateBack(): Unit {
        val intent = when (returnTo) {
            "SettingsActivity" -> Intent(this, SettingsActivity::class.java)
            "CategoryActivity" -> Intent(this, CategoryActivity::class.java)
            "AddRecipeActivity" -> Intent(this, AddRecipeActivity::class.java)
            "RecipeDetailActivity" -> Intent(this, RecipeDetailActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
    }
}

