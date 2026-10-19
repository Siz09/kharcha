package com.sijan.kharcha.ui.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.R
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(EntryType.EXPENSE)
    private val isAddDialogOpen = MutableStateFlow(false)
    private val editingCategory = MutableStateFlow<Category?>(null)
    private val deletingCategory = MutableStateFlow<Category?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        ledgerRepository.getAllCategories(),
        selectedTab,
        isAddDialogOpen,
        editingCategory,
        deletingCategory
    ) { categories, tab, isAdd, editCat, delCat ->
        val expense = categories.filter { it.type == EntryType.EXPENSE }
        val income = categories.filter { it.type == EntryType.INCOME }

        CategoriesUiState(
            selectedTab = tab,
            expenseCategories = expense,
            incomeCategories = income,
            isAddDialogOpen = isAdd,
            editingCategory = editCat,
            deletingCategory = delCat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    fun selectTab(type: EntryType) {
        selectedTab.value = type
    }

    fun openAddDialog() {
        isAddDialogOpen.value = true
    }

    fun dismissAddDialog() {
        isAddDialogOpen.value = false
    }

    fun openEditDialog(category: Category) {
        editingCategory.value = category
    }

    fun dismissEditDialog() {
        editingCategory.value = null
    }

    fun openDeleteDialog(category: Category) {
        deletingCategory.value = category
    }

    fun dismissDeleteDialog() {
        deletingCategory.value = null
    }

    fun addCategory(name: String): Job = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch

        ledgerRepository.insertCategory(name = trimmed, type = selectedTab.value)
        dismissAddDialog()
    }

    fun renameCategory(id: Long, newName: String): Job = viewModelScope.launch {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@launch

        ledgerRepository.renameCategory(id = id, newName = trimmed)
        dismissEditDialog()
    }

    fun deleteCategory(id: Long, onError: (Int) -> Unit): Job = viewModelScope.launch {
        ledgerRepository.deleteCategory(id)
            .onSuccess {
                dismissDeleteDialog()
            }
            .onFailure { error ->
                dismissDeleteDialog()
                if (error is IllegalStateException) {
                    onError(R.string.categories_error_in_use)
                }
            }
    }
}
