package com.beomjo.whitenoise.repositories.category

import com.beomjo.whitenoise.model.Category

interface CategoryRepository {
    suspend fun getCategories(documentPath: String): List<Category>
}