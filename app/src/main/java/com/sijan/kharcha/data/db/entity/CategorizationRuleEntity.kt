package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.CategorizationRule

@Entity(
    tableName = "categorization_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index("priority")
    ]
)
data class CategorizationRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val categoryId: Long,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CategorizationRule = CategorizationRule(
        id = id,
        name = name,
        pattern = pattern,
        isRegex = isRegex,
        categoryId = categoryId,
        priority = priority,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(rule: CategorizationRule): CategorizationRuleEntity = CategorizationRuleEntity(
            id = rule.id,
            name = rule.name,
            pattern = rule.pattern,
            isRegex = rule.isRegex,
            categoryId = rule.categoryId,
            priority = rule.priority,
            isActive = rule.isActive,
            createdAt = rule.createdAt,
            updatedAt = rule.updatedAt
        )
    }
}
