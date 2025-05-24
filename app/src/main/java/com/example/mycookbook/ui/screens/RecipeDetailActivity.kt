package com.example.mycookbook.ui.screens

import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mycookbook.R

class RecipeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val categoryId = intent.getIntExtra("categoryId", -1)
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)

        when (categoryId) {
            1 -> rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.breakfast_yellow))
            2 -> rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lunch_purple))
            3 -> rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.dessert_peach))
            4 -> rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.drinks_blue))
        }
    }
}
