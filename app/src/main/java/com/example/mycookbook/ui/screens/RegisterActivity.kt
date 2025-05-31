package com.example.mycookbook.ui.screens

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.mycookbook.R
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.User
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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

            if (firstName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByEmail(email)
                if(existingUser != null) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val newUser = User(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = password
                    )
                    db.userDao().insert(newUser)
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Registered $firstName successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}
