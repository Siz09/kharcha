package com.sijan.kharcha.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sijan.kharcha.data.db.converters.Converters
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryFtsEntity

@Database(
    entities = [
        CategoryEntity::class,
        LedgerEntryEntity::class,
        LedgerEntryFtsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        const val DATABASE_NAME = "kharcha.db"
    }
}
