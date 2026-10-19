package com.sijan.kharcha.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun FieldWithStatus(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    status: ValidationStatus = ValidationStatus.VALID,
    statusMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isError = status == ValidationStatus.ERROR
    val isWarning = status == ValidationStatus.WARNING && !statusMessage.isNullOrBlank()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        placeholder = placeholder?.let { { Text(text = it) } },
        isError = isError,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon ?: if (isWarning) {
            {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        } else null,
        supportingText = when {
            isError && !statusMessage.isNullOrBlank() -> {
                {
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            isWarning && !statusMessage.isNullOrBlank() -> {
                {
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            else -> null
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun FieldWithStatusPreviewLight() {
    KharchaTheme {
        FieldWithStatus(
            value = "12345",
            onValueChange = {},
            label = "Transaction ID",
            status = ValidationStatus.WARNING,
            statusMessage = "Unverified transaction ID",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FieldWithStatusPreviewDark() {
    KharchaTheme {
        FieldWithStatus(
            value = "",
            onValueChange = {},
            label = "Amount",
            status = ValidationStatus.ERROR,
            statusMessage = "Amount must be greater than zero",
            modifier = Modifier.padding(16.dp)
        )
    }
}
