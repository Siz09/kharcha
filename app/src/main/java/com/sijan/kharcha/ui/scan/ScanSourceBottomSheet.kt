package com.sijan.kharcha.ui.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSourceBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onPickFromGallery: () -> Unit,
    onScanWithCamera: () -> Unit,
    onPickDocument: () -> Unit = {},
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.scan_source_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ScanSourceOptionItem(
                icon = Icons.Default.PhotoLibrary,
                title = stringResource(R.string.scan_source_photo_picker),
                description = stringResource(R.string.scan_source_photo_picker_desc),
                onClick = {
                    onDismissRequest()
                    onPickFromGallery()
                }
            )

            ScanSourceOptionItem(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.scan_source_camera),
                description = stringResource(R.string.scan_source_camera_desc),
                onClick = {
                    onDismissRequest()
                    onScanWithCamera()
                }
            )

            ScanSourceOptionItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.scan_pick_document),
                description = stringResource(R.string.scan_source_document_desc),
                onClick = {
                    onDismissRequest()
                    onPickDocument()
                }
            )

            ScanSourceOptionItem(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.scan_source_manual),
                description = stringResource(R.string.scan_source_manual_desc),
                onClick = {
                    onDismissRequest()
                    onManualEntry()
                }
            )
        }
    }
}

@Composable
private fun ScanSourceOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
