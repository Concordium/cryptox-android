package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `contract_token_table` ADD COLUMN `added_at` INTEGER NOT NULL DEFAULT 0")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `protocol_level_token_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tokenId` TEXT NOT NULL,
                `tokenState` TEXT,
                `tokenAccountState` TEXT,
                `account_address` TEXT,
                `token_balance` TEXT NOT NULL,
                `added_at` INTEGER NOT NULL DEFAULT 0,
                `is_hidden` INTEGER NOT NULL DEFAULT 0,
                `is_newly_received` INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        database.execSQL(
            """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_protocol_level_token_table_tokenId` 
                ON `protocol_level_token_table` (`tokenId`)
                """
        )

        // Base timestamp: Jan 1, 2000 UTC
        val baseTime = 946684800000L

        val cursor = database.query("SELECT id FROM contract_token_table")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val addedAt = baseTime + id * 60_000 // 1 minute per ID
            database.execSQL("UPDATE contract_token_table SET added_at = $addedAt WHERE id = $id")
        }
        cursor.close()
    }
}