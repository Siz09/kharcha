package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entityType: String, // "ENTRY", "BILL", "CATEGORY"
    val entityId: Long,
    val action: String,     // "INSERT", "UPDATE", "DELETE"
    val details: String,    // Human-readable summary or JSON
    val timestamp: Long = System.currentTimeMillis()
)
