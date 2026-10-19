package com.sijan.kharcha.ui.components

import androidx.annotation.DrawableRes
import com.sijan.kharcha.R

/**
 * Maps category names to Animotion vector drawables.
 */
object CategoryIconHelper {
    @DrawableRes
    fun getIconRes(categoryName: String): Int {
        val lower = categoryName.lowercase().trim()
        return when {
            lower.contains("education") || lower.contains("school") || lower.contains("college") -> R.drawable.ic_cat_education
            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("coffee") -> R.drawable.ic_cat_food
            lower.contains("grocer") || lower.contains("market") || lower.contains("supermarket") -> R.drawable.ic_cat_groceries
            lower.contains("transport") || lower.contains("travel") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("bus") || lower.contains("taxi") -> R.drawable.ic_cat_transport
            lower.contains("health") || lower.contains("medic") || lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("doctor") -> R.drawable.ic_cat_health
            lower.contains("entertain") || lower.contains("movie") || lower.contains("game") || lower.contains("music") -> R.drawable.ic_cat_entertainment
            lower.contains("shop") || lower.contains("cloth") || lower.contains("buy") -> R.drawable.ic_cat_shopping
            else -> R.drawable.ic_cat_others
        }
    }
}
