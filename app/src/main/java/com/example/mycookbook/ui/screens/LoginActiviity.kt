package com.example.mycookbook.ui.screens

import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val firstName = findViewById<EditText>(R.id.etFirstName).text.toString()
            val lastName = findViewById<EditText>(R.id.etLastName).text.toString()
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()

            val newUser = User(firstName = firstName, lastName = lastName, email = email, password = password)
            userViewModel.add(newUser)

            Toast.makeText(this, "Registered!", Toast.LENGTH_SHORT).show()
            finish() // or go to login screen
        }
    }
}
