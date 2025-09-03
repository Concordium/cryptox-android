package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `contract_token_table` ADD COLUMN `added_at` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("CREATE TABLE `new_transfer_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account_id` INTEGER NOT NULL, `amount` TEXT NOT NULL, `cost` TEXT NOT NULL, `from_address` TEXT NOT NULL, `to_address` TEXT NOT NULL, `expiry` INTEGER NOT NULL, `memo` TEXT, `created_at` INTEGER NOT NULL, `submission_id` TEXT NOT NULL, `transaction_status` INTEGER NOT NULL, `outcome` INTEGER NOT NULL, `transaction_type` TEXT NOT NULL, `token_transfer_amount` TEXT, `token_symbol` TEXT)");
        db.execSQL("INSERT INTO `new_transfer_table` (`id`, `account_id`, `amount`, `cost`, " +
                "`from_address`, `to_address`, `expiry`, `memo`, `created_at`, `submission_id`, " +
                "`transaction_status`, `outcome`, `transaction_type`, " +
                "`token_transfer_amount`, `token_symbol`) " +
                "SELECT `id`, `account_id`, `amount`, `cost`, " +
                "`from_address`, `to_address`, `expiry`, `memo`, `created_at`, `submission_id`, " +
                "`transaction_status`, `outcome`, `transactionType`, " +
                "NULL as `token_transfer_amount`, NULL as `token_symbol` " +
                "FROM `transfer_table`")
        db.execSQL("DROP TABLE `transfer_table`")
        db.execSQL("ALTER TABLE `new_transfer_table` RENAME TO `transfer_table`")

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
