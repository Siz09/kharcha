package com.sijan.kharcha.ui.settings.categories

import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType

data class CategoriesUiState(
    val selectedTab: EntryType = EntryType.EXPENSE,
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val editingCategory: Category? = null,
    val deletingCategory: Category? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
) {
    val currentCategories: List<Category>
        get() = when (selectedTab) {
            EntryType.EXPENSE -> expenseCategories
            EntryType.INCOME -> incomeCategories
        }
}
