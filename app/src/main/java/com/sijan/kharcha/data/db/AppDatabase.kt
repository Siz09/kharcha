package com.sijan.kharcha.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sijan.kharcha.data.db.converters.Converters
import com.sijan.kharcha.data.db.dao.AccountDao
import com.sijan.kharcha.data.db.dao.AuditLogDao
import com.sijan.kharcha.data.db.dao.BillDao
import com.sijan.kharcha.data.db.dao.BudgetDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.dao.RecurringEntryDao
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.AuditLogEntity
import com.sijan.kharcha.data.db.entity.BillEntity
import com.sijan.kharcha.data.db.entity.BudgetEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryFtsEntity
import com.sijan.kharcha.data.db.entity.RecurringEntryEntity

@Database(
    entities = [
        CategoryEntity::class,
        LedgerEntryEntity::class,
        LedgerEntryFtsEntity::class,
        BillEntity::class,
        BudgetEntity::class,
        AuditLogEntity::class,
        RecurringEntryEntity::class,
        AccountEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun billDao(): BillDao
    abstract fun budgetDao(): BudgetDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun recurringEntryDao(): RecurringEntryDao
    abstract fun accountDao(): AccountDao

    companion object {
        const val DATABASE_NAME = "kharcha.db"
    }
}
