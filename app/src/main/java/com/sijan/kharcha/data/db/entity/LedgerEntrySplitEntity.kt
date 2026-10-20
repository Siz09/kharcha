package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.SplitItem

@Entity(
    tableName = "ledger_entry_splits",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("entryId"),
        Index("categoryId")
    ]
)
data class LedgerEntrySplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entryId: Long,
    val categoryId: Long,
    val amountPaisa: Long,
    val note: String? = null
) {
    fun toDomain(): SplitItem = SplitItem(
        id = id,
        entryId = entryId,
        categoryId = categoryId,
        amountPaisa = amountPaisa,
        note = note
    )

    companion object {
        fun fromDomain(item: SplitItem, entryId: Long = item.entryId): LedgerEntrySplitEntity = LedgerEntrySplitEntity(
            id = item.id,
            entryId = entryId,
            categoryId = item.categoryId,
            amountPaisa = item.amountPaisa,
            note = item.note
        )
    }
}
