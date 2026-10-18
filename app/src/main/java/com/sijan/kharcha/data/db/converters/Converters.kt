package com.sijan.kharcha.data.db.converters

import androidx.room.TypeConverter
import com.sijan.kharcha.domain.model.EntryType

class Converters {

    @TypeConverter
    fun fromEntryType(type: EntryType): String {
        return type.name
    }

    @TypeConverter
    fun toEntryType(value: String): EntryType {
        return EntryType.valueOf(value)
    }
}
