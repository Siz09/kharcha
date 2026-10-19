package com.sijan.kharcha.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `bills` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `amountPaisa` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `dueDayOfMonth` INTEGER NOT NULL,
                `frequency` TEXT NOT NULL,
                `isAutoReminder` INTEGER NOT NULL,
                `lastPaidDate` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_bills_categoryId` ON `bills` (`categoryId`)
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `monthYear` TEXT NOT NULL,
                `limitPaisa` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId_monthYear` ON `budgets` (`categoryId`, `monthYear`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `audit_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entityType` TEXT NOT NULL,
                `entityId` INTEGER NOT NULL,
                `action` TEXT NOT NULL,
                `details` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_audit_logs_entityType_entityId` ON `audit_logs` (`entityType`, `entityId`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_audit_logs_timestamp` ON `audit_logs` (`timestamp`)
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recurring_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amountPaisa` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `nextDueDate` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_recurring_entries_categoryId` ON `recurring_entries` (`categoryId`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_recurring_entries_nextDueDate` ON `recurring_entries` (`nextDueDate`)
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `institutionName` TEXT,
                `accountNumberLast4` TEXT,
                `openingBalancePaisa` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_accounts_type` ON `accounts` (`type`)
        """.trimIndent())

        val now = System.currentTimeMillis()
        db.execSQL("""
            INSERT OR IGNORE INTO `accounts` (`id`, `name`, `type`, `institutionName`, `accountNumberLast4`, `openingBalancePaisa`, `isActive`, `createdAt`, `updatedAt`)
            VALUES (1, 'Cash in Hand', 'CASH', NULL, NULL, 0, 1, $now, $now)
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE `ledger_entries` ADD COLUMN `accountId` INTEGER NOT NULL DEFAULT 1 REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE `ledger_entries` ADD COLUMN `toAccountId` INTEGER DEFAULT NULL REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_ledger_entries_accountId` ON `ledger_entries` (`accountId`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_ledger_entries_toAccountId` ON `ledger_entries` (`toAccountId`)
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `udhars` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `personName` TEXT NOT NULL,
                `phoneNumber` TEXT,
                `type` TEXT NOT NULL,
                `amountPaisa` INTEGER NOT NULL,
                `settledPaisa` INTEGER NOT NULL,
                `dueDate` INTEGER,
                `notes` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_udhars_personName` ON `udhars` (`personName`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_udhars_type` ON `udhars` (`type`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_udhars_status` ON `udhars` (`status`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_udhars_dueDate` ON `udhars` (`dueDate`)
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `udhar_settlements` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `udharId` INTEGER NOT NULL,
                `amountPaisa` INTEGER NOT NULL,
                `dateEpochMillis` INTEGER NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`udharId`) REFERENCES `udhars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_udhar_settlements_udharId` ON `udhar_settlements` (`udharId`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_udhar_settlements_dateEpochMillis` ON `udhar_settlements` (`dateEpochMillis`)
        """.trimIndent())
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `targetAmountPaisa` INTEGER NOT NULL,
                `savedAmountPaisa` INTEGER NOT NULL,
                `targetDateEpochMillis` INTEGER,
                `notes` TEXT NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_savings_goals_name` ON `savings_goals` (`name`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_savings_goals_isCompleted` ON `savings_goals` (`isCompleted`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_savings_goals_targetDateEpochMillis` ON `savings_goals` (`targetDateEpochMillis`)
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_contributions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `goalId` INTEGER NOT NULL,
                `amountPaisa` INTEGER NOT NULL,
                `dateEpochMillis` INTEGER NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_savings_contributions_goalId` ON `savings_contributions` (`goalId`)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_savings_contributions_dateEpochMillis` ON `savings_contributions` (`dateEpochMillis`)
        """.trimIndent())
    }
}



