package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.AccountReconciliation

@Entity(
    tableName = "account_reconciliations",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("statementDate")
    ]
)
data class AccountReconciliationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountId: Long,
    val statementDate: Long,
    val statementBalancePaisa: Long,
    val computedBalancePaisa: Long,
    val differencePaisa: Long,
    val isReconciled: Boolean,
    val reconciledAt: Long,
    val notes: String? = null
) {
    fun toDomain(): AccountReconciliation = AccountReconciliation(
        id = id,
        accountId = accountId,
        statementDate = statementDate,
        statementBalancePaisa = statementBalancePaisa,
        computedBalancePaisa = computedBalancePaisa,
        differencePaisa = differencePaisa,
        isReconciled = isReconciled,
        reconciledAt = reconciledAt,
        notes = notes
    )

    companion object {
        fun fromDomain(rec: AccountReconciliation): AccountReconciliationEntity = AccountReconciliationEntity(
            id = rec.id,
            accountId = rec.accountId,
            statementDate = rec.statementDate,
            statementBalancePaisa = rec.statementBalancePaisa,
            computedBalancePaisa = rec.computedBalancePaisa,
            differencePaisa = rec.differencePaisa,
            isReconciled = rec.isReconciled,
            reconciledAt = rec.reconciledAt,
            notes = rec.notes
        )
    }
}
