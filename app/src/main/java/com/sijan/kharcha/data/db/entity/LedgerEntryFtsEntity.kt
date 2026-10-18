package com.sijan.kharcha.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = LedgerEntryEntity::class)
@Entity(tableName = "ledger_entries_fts")
data class LedgerEntryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long = 0L,
    val note: String
)
