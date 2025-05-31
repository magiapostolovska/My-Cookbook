package com.example.mycookbook.ui.screens

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.mycookbook.R

class RecipeDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val categoryId = intent.getIntExtra("recipe_category_id", -1)
        val layoutResId = when (categoryId) {
            1 -> R.layout.activity_recipe_detail
            2 -> R.layout.activity_recipe_detail2
            3 -> R.layout.activity_recipe_detail3
            4 -> R.layout.activity_recipe_detail4
            else -> R.layout.activity_recipe_detail
        }
        setContentView(layoutResId)

        val title = intent.getStringExtra("recipe_title") ?: "No Title"
        val ingredients = intent.getStringExtra("recipe_ingredients") ?: "No Ingredients"
        val instructions = intent.getStringExtra("recipe_instructions") ?: "No Instructions"

        findViewById<TextView>(R.id.tvTitle).text = title
        findViewById<TextView>(R.id.tvIngredients).text = ingredients
        findViewById<TextView>(R.id.tvInstructions).text = instructions

        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            finish()
        }
    }
}
