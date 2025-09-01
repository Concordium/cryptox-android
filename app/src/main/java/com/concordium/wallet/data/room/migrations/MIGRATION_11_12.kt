package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `contract_token_table` ADD COLUMN `added_at` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `transfer_table` ADD COLUMN `token_transfer_amount` TEXT DEFAULT null")
        db.execSQL("ALTER TABLE `transfer_table` ADD COLUMN `token_symbol` TEXT DEFAULT null")
        db.execSQL("ALTER TABLE `transfer_table` DROP COLUMN `newSelfEncryptedAmount`")
        db.execSQL("ALTER TABLE `transfer_table` DROP COLUMN `newStartIndex`")
        db.execSQL("ALTER TABLE `transfer_table` DROP COLUMN `nonce`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `protocol_level_token_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tokenId` TEXT NOT NULL,
                `token_metadata` TEXT,
                `account_address` TEXT,
                `token_balance` TEXT NOT NULL,
                `added_at` INTEGER NOT NULL DEFAULT 0,
                `is_hidden` INTEGER NOT NULL DEFAULT 0,
                `is_newly_received` INTEGER NOT NULL DEFAULT 0,
                `is_in_allow_list` INTEGER DEFAULT null,
                `is_in_deny_list` INTEGER DEFAULT null
            )
            """
        )

        // Base timestamp: Jan 1, 2000 UTC
        val baseTime = 946684800000L

        val cursor = db.query("SELECT id FROM contract_token_table")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val addedAt = baseTime + id * 60_000 // 1 minute per ID
            db.execSQL("UPDATE contract_token_table SET added_at = $addedAt WHERE id = $id")
        }
        cursor.close()
    }
}
