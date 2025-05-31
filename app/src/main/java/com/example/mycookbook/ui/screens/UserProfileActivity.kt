package com.example.mycookbook.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.mycookbook.MainActivity
import com.example.mycookbook.R
import com.example.mycookbook.data.local.entity.User
import com.example.mycookbook.viewmodel.UserViewModel

class UserProfileActivity : ComponentActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var ivEdit: ImageView
    private lateinit var btnSave: Button
    private lateinit var ivSettings: ImageView

    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText

    private lateinit var userViewModel: UserViewModel

    private var currentUser: User? = null
    private var isEditing = false
    private var returnTo: String? = null
    private var categoryId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        returnTo = intent.getStringExtra("return_to")
        categoryId = intent.getIntExtra("category_id", -1)

        ivBackArrow = findViewById(R.id.ivBackArrow)
        ivEdit = findViewById(R.id.ivEdit)
        btnSave = findViewById(R.id.btnSave)
        ivSettings = findViewById(R.id.ivSettings)

        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)

        userViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(UserViewModel::class.java)

        userViewModel.loadAll()
        userViewModel.users.observe(this, Observer { users ->
            currentUser = users.find { it.id == 1 } ?: users.firstOrNull()
            currentUser?.let { populateUserInfo(it) }
        })

        ivBackArrow.setOnClickListener {
            if (isEditing) {
                currentUser?.let { populateUserInfo(it) }
                toggleEditMode(false)
            } else {
                val intent = when (returnTo) {
                    "SettingsActivity" -> Intent(this, SettingsActivity::class.java)
                    "CategoryActivity" -> Intent(this, CategoryActivity::class.java)
                    "AddRecipeActivity" -> Intent(this, AddRecipeActivity::class.java)
                    else -> Intent(this, MainActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

        ivEdit.setOnClickListener {
            toggleEditMode(true)
        }

        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("return_to", "UserProfileActivity")
            startActivity(intent)
        }

        btnSave.setOnClickListener {
            saveUserData()
        }

        toggleEditMode(false)
    }

    private fun populateUserInfo(user: User) {
        tvFirstName.text = user.firstName
        tvLastName.text = user.lastName
        tvEmail.text = user.email

        etFirstName.setText(user.firstName)
        etLastName.setText(user.lastName)
        etEmail.setText(user.email)
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable

        if (enable) {
            tvFirstName.visibility = View.GONE
            tvLastName.visibility = View.GONE
            tvEmail.visibility = View.GONE

            etFirstName.visibility = View.VISIBLE
            etLastName.visibility = View.VISIBLE
            etEmail.visibility = View.VISIBLE

            btnSave.visibility = View.VISIBLE
            ivEdit.visibility = View.GONE
        } else {
            tvFirstName.visibility = View.VISIBLE
            tvLastName.visibility = View.VISIBLE
            tvEmail.visibility = View.VISIBLE

            etFirstName.visibility = View.GONE
            etLastName.visibility = View.GONE
            etEmail.visibility = View.GONE

            btnSave.visibility = View.GONE
            ivEdit.visibility = View.VISIBLE
        }
    }

    private fun saveUserData() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val user = currentUser
        if (user != null) {
            val updatedUser = user.copy(
                firstName = firstName,
                lastName = lastName,
                email = email
            )
            userViewModel.update(updatedUser)
            currentUser = updatedUser

            populateUserInfo(updatedUser)
            toggleEditMode(false)
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "User not loaded", Toast.LENGTH_SHORT).show()
        }
    }
}
