package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.TaxCategory

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("toAccountId"),
        Index("date"),
        Index("transactionId")
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amountPaisa: Long,
    val type: EntryType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val toAccountId: Long? = null,
    val note: String = "",
    val date: Long,
    val source: String = "manual",
    val receiptPath: String? = null,
    val rawOcrText: String? = null,
    val transactionId: String? = null,
    val isTaxDeductible: Boolean = false,
    val taxCategory: TaxCategory? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): LedgerEntry = LedgerEntry(
        id = id,
        amountPaisa = amountPaisa,
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        toAccountId = toAccountId,
        note = note,
        date = date,
        source = source,
        receiptPath = receiptPath,
        rawOcrText = rawOcrText,
        transactionId = transactionId,
        isTaxDeductible = isTaxDeductible,
        taxCategory = taxCategory,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(entry: LedgerEntry): LedgerEntryEntity = LedgerEntryEntity(
            id = entry.id,
            amountPaisa = entry.amountPaisa,
            type = entry.type,
            categoryId = entry.categoryId,
            accountId = entry.accountId,
            toAccountId = entry.toAccountId,
            note = entry.note,
            date = entry.date,
            source = entry.source,
            receiptPath = entry.receiptPath,
            rawOcrText = entry.rawOcrText,
            transactionId = entry.transactionId,
            isTaxDeductible = entry.isTaxDeductible,
            taxCategory = entry.taxCategory,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt
        )
    }
}
