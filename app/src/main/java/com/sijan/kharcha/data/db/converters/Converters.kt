package com.sijan.kharcha.data.db.converters

import androidx.room.TypeConverter
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.DocumentType
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.TaxCategory
import com.sijan.kharcha.domain.model.UtilityType

class Converters {

    @TypeConverter
    fun fromEntryType(type: EntryType): String {
        return type.name
    }

    @TypeConverter
    fun toEntryType(value: String): EntryType {
        return EntryType.valueOf(value)
    }

    @TypeConverter
    fun fromAccountType(type: AccountType): String {
        return type.name
    }

    @TypeConverter
    fun toAccountType(value: String): AccountType {
        return AccountType.valueOf(value)
    }

    @TypeConverter
    fun fromUtilityType(type: UtilityType): String = type.name

    @TypeConverter
    fun toUtilityType(value: String): UtilityType = UtilityType.valueOf(value)

    @TypeConverter
    fun fromTaxCategory(category: TaxCategory?): String? = category?.name

    @TypeConverter
    fun toTaxCategory(value: String?): TaxCategory? = value?.let {
        try {
            TaxCategory.valueOf(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    @TypeConverter
    fun fromDocumentType(type: DocumentType?): String? = type?.name

    @TypeConverter
    fun toDocumentType(value: String?): DocumentType? = value?.let {
        try {
            DocumentType.valueOf(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
