package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name", "type"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: EntryType
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        type = type
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            type = category.type
        )
    }
}
