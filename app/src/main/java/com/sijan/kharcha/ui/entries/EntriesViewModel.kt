package com.sijan.kharcha.ui.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.EntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<EntryType?>(null)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }

    private val entriesFlow = combine(
        _searchQuery,
        _selectedType,
        _selectedCategoryId
    ) { query, type, catId ->
        Triple(query, type, catId)
    }.flatMapLatest { (query, type, catId) ->
        if (query.isNotBlank()) {
            repository.searchEntries(query.trim()).map { list ->
                var filtered = list
                if (type != null) filtered = filtered.filter { it.type == type }
                if (catId != null) filtered = filtered.filter { it.categoryId == catId }
                filtered
            }
        } else {
            repository.getFilteredEntries(type = type, categoryId = catId)
        }
    }

    val uiState: StateFlow<EntriesUiState> = combine(
        entriesFlow,
        repository.getAllCategories(),
        _searchQuery,
        _selectedType,
        _selectedCategoryId
    ) { entries, categories, query, type, catId ->
        EntriesUiState(
            searchQuery = query,
            selectedType = type,
            selectedCategoryId = catId,
            entries = entries,
            categoriesMap = categories.associateBy { it.id },
            categories = categories,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EntriesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeFilterChange(type: EntryType?) {
        _selectedType.value = type
    }

    fun onCategoryFilterChange(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }
}
