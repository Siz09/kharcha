package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.Account
import com.sijan.kharcha.domain.model.AccountType

@Entity(
    tableName = "accounts",
    indices = [
        Index("name"),
        Index("type")
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val institutionName: String? = null,
    val accountNumberLast4: String? = null,
    val openingBalancePaisa: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        type = type,
        institutionName = institutionName,
        accountNumberLast4 = accountNumberLast4,
        openingBalancePaisa = openingBalancePaisa,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type,
            institutionName = account.institutionName,
            accountNumberLast4 = account.accountNumberLast4,
            openingBalancePaisa = account.openingBalancePaisa,
            isActive = account.isActive,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
