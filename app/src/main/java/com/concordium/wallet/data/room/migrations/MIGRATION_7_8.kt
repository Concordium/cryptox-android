package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE account_contract_table")
        database.execSQL("ALTER TABLE `contract_token_table` ADD COLUMN `is_newly_received` INTEGER NOT NULL DEFAULT 0")
    }
}
