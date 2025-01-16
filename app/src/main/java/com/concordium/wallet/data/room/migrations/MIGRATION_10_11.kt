package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `account_table` ADD COLUMN `is_active` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("""
            UPDATE `account_table`
            SET `is_active` = 1
            WHERE `id` = (
                SELECT `id` 
                FROM `account_table`
                ORDER BY `id` ASC
                LIMIT 1
            )
        """)
    }
}