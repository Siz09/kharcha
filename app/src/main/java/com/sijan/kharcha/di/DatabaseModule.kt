package com.sijan.kharcha.di

import android.content.Context
import androidx.room.Room
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.AccountDao
import com.sijan.kharcha.data.db.dao.AuditLogDao
import com.sijan.kharcha.data.db.dao.BillDao
import com.sijan.kharcha.data.db.dao.BudgetDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.dao.RecurringEntryDao
import com.sijan.kharcha.data.db.dao.SavingsGoalDao
import com.sijan.kharcha.data.db.dao.UdharDao
import com.sijan.kharcha.data.db.dao.UtilityAccountDao
import com.sijan.kharcha.data.db.migration.MIGRATION_1_2
import com.sijan.kharcha.data.db.migration.MIGRATION_2_3
import com.sijan.kharcha.data.db.migration.MIGRATION_3_4
import com.sijan.kharcha.data.db.migration.MIGRATION_4_5
import com.sijan.kharcha.data.db.migration.MIGRATION_5_6
import com.sijan.kharcha.data.db.migration.MIGRATION_6_7
import com.sijan.kharcha.data.repository.AnalyticsRepository
import com.sijan.kharcha.data.repository.AnalyticsRepositoryImpl
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import com.sijan.kharcha.data.repository.SavingsGoalRepository
import com.sijan.kharcha.data.repository.SavingsGoalRepositoryImpl
import com.sijan.kharcha.data.repository.UdharRepository
import com.sijan.kharcha.data.repository.UdharRepositoryImpl
import com.sijan.kharcha.data.repository.UtilityRepository
import com.sijan.kharcha.data.repository.UtilityRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideLedgerDao(database: AppDatabase): LedgerDao {
        return database.ledgerDao()
    }

    @Provides
    fun provideBillDao(database: AppDatabase): BillDao {
        return database.billDao()
    }

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideAuditLogDao(database: AppDatabase): AuditLogDao {
        return database.auditLogDao()
    }

    @Provides
    fun provideRecurringEntryDao(database: AppDatabase): RecurringEntryDao {
        return database.recurringEntryDao()
    }

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideUdharDao(database: AppDatabase): UdharDao {
        return database.udharDao()
    }

    @Provides
    fun provideSavingsGoalDao(database: AppDatabase): SavingsGoalDao {
        return database.savingsGoalDao()
    }

    @Provides
    fun provideUtilityAccountDao(database: AppDatabase): UtilityAccountDao {
        return database.utilityAccountDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLedgerRepository(
        ledgerRepositoryImpl: LedgerRepositoryImpl
    ): LedgerRepository

    @Binds
    @Singleton
    abstract fun bindUdharRepository(
        udharRepositoryImpl: UdharRepositoryImpl
    ): UdharRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(
        savingsGoalRepositoryImpl: SavingsGoalRepositoryImpl
    ): SavingsGoalRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        analyticsRepositoryImpl: AnalyticsRepositoryImpl
    ): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindUtilityRepository(
        utilityRepositoryImpl: UtilityRepositoryImpl
    ): UtilityRepository
}

