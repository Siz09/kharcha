package com.sijan.kharcha.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class ArchiveAsset(
    val relativePath: String,
    val sha256: String,
    val sizeBytes: Long
)

@Serializable
data class ArchiveManifest(
    val archiveVersion: Int = 1,
    val schemaVersion: Int,
    val createdAt: Long,
    val appVersion: String,
    val deviceModel: String? = null,
    val totalEntries: Int,
    val totalAccounts: Int,
    val totalCategories: Int,
    val totalAssets: Int,
    val assets: List<ArchiveAsset> = emptyList()
)
