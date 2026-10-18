package com.sijan.kharcha.data.backup.model

import com.sijan.kharcha.domain.model.EntryType
import kotlinx.serialization.Serializable

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val type: EntryType
)
