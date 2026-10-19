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
