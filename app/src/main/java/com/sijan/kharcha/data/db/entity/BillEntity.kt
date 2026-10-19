package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.Bill

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId")
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val amountPaisa: Long,
    val categoryId: Long,
    val dueDayOfMonth: Int,
    val frequency: String = "MONTHLY",
    val isAutoReminder: Boolean = true,
    val lastPaidDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Bill = Bill(
        id = id,
        name = name,
        amountPaisa = amountPaisa,
        categoryId = categoryId,
        dueDayOfMonth = dueDayOfMonth,
        frequency = frequency,
        isAutoReminder = isAutoReminder,
        lastPaidDate = lastPaidDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(bill: Bill): BillEntity = BillEntity(
            id = bill.id,
            name = bill.name,
            amountPaisa = bill.amountPaisa,
            categoryId = bill.categoryId,
            dueDayOfMonth = bill.dueDayOfMonth,
            frequency = bill.frequency,
            isAutoReminder = bill.isAutoReminder,
            lastPaidDate = bill.lastPaidDate,
            createdAt = bill.createdAt,
            updatedAt = bill.updatedAt
        )
    }
}
