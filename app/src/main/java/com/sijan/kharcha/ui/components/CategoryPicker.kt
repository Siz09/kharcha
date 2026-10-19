package com.sijan.kharcha.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.ui.theme.KharchaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val displayText = selectedCategory?.name ?: ""

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                label = { Text(text = stringResource(R.string.field_category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                isError = isError,
                supportingText = if (isError && errorMessage != null) {
                    { Text(text = errorMessage) }
                } else null,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CategoryPickerPreviewLight() {
    KharchaTheme {
        val sampleCategories = listOf(
            Category(id = 1L, name = "Food & Dining", type = EntryType.EXPENSE),
            Category(id = 2L, name = "Shopping", type = EntryType.EXPENSE),
            Category(id = 3L, name = "Bills & Utilities", type = EntryType.EXPENSE)
        )
        CategoryPicker(
            categories = sampleCategories,
            selectedCategoryId = 1L,
            onCategorySelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CategoryPickerPreviewDark() {
    KharchaTheme {
        val sampleCategories = listOf(
            Category(id = 1L, name = "Food & Dining", type = EntryType.EXPENSE),
            Category(id = 2L, name = "Shopping", type = EntryType.EXPENSE)
        )
        CategoryPicker(
            categories = sampleCategories,
            selectedCategoryId = null,
            onCategorySelected = {},
            isError = true,
            errorMessage = "Please select a category",
            modifier = Modifier.padding(16.dp)
        )
    }
}
