package com.sijan.kharcha.ui.entries

import androidx.compose.runtime.Immutable
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry

@Immutable
data class EntriesUiState(
    val searchQuery: String = "",
    val selectedType: EntryType? = null,
    val selectedCategoryId: Long? = null,
    val entries: List<LedgerEntry> = emptyList(),
    val categoriesMap: Map<Long, Category> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)
