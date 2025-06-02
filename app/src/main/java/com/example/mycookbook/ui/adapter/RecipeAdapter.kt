package com.example.mycookbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycookbook.R
import com.example.mycookbook.data.local.entity.Recipe

class RecipeAdapter(
    private val recipes: List<Recipe>,
    private val onArrowClick: (Recipe) -> Unit,
    private val onItemClick: ((Recipe) -> Unit)? = null
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.tvRecipeTitle)
        private val arrowImageView: ImageView = view.findViewById(R.id.ivArrow)

        fun bind(recipe: Recipe, position: Int) {
            val numberedTitle = "${position + 1}. ${recipe.title}"
            titleTextView.text = numberedTitle

            arrowImageView.setOnClickListener {
                onArrowClick(recipe)
            }

            onItemClick?.let { itemClick ->
                itemView.setOnClickListener {
                    itemClick(recipe)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recipes[position], position)
    }

    override fun getItemCount() = recipes.size
}
