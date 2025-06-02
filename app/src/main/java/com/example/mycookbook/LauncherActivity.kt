package com.example.mycookbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.mycookbook.ui.screens.LoginActivity

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = checkIfLoggedIn()

        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }

    private fun checkIfLoggedIn(): Boolean {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return prefs.getBoolean("isLoggedIn", false)
    }
}

