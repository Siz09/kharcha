package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType

@Entity(tableName = "utility_accounts")
data class UtilityAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val utilityType: UtilityType,
    val providerName: String,
    val consumerId: String,
    val counterOrBranch: String? = null,
    val accountHolderName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UtilityAccount = UtilityAccount(
        id = id,
        utilityType = utilityType,
        providerName = providerName,
        consumerId = consumerId,
        counterOrBranch = counterOrBranch,
        accountHolderName = accountHolderName,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(account: UtilityAccount): UtilityAccountEntity = UtilityAccountEntity(
            id = account.id,
            utilityType = account.utilityType,
            providerName = account.providerName,
            consumerId = account.consumerId,
            counterOrBranch = account.counterOrBranch,
            accountHolderName = account.accountHolderName,
            notes = account.notes,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
