package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE `new_transfer_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account_id` INTEGER NOT NULL, `amount` TEXT NOT NULL, `cost` TEXT NOT NULL, `from_address` TEXT NOT NULL, `to_address` TEXT NOT NULL, `expiry` INTEGER NOT NULL, `memo` TEXT, `created_at` INTEGER NOT NULL, `submission_id` TEXT NOT NULL, `transaction_status` INTEGER NOT NULL, `outcome` INTEGER NOT NULL, `transaction_type` TEXT NOT NULL, `token_transfer_amount` TEXT, `token_symbol` TEXT)")
        db.execSQL(
            "INSERT INTO `new_transfer_table` (`id`, `account_id`, `amount`, `cost`, " +
                    "`from_address`, `to_address`, `expiry`, `memo`, `created_at`, `submission_id`, " +
                    "`transaction_status`, `outcome`, `transaction_type`, " +
                    "`token_transfer_amount`, `token_symbol`) " +
                    "SELECT `id`, `account_id`, `amount`, `cost`, " +
                    "`from_address`, `to_address`, `expiry`, `memo`, `created_at`, `submission_id`, " +
                    "`transaction_status`, `outcome`, `transactionType`, " +
                    "NULL as `token_transfer_amount`, NULL as `token_symbol` " +
                    "FROM `transfer_table`"
        )
        db.execSQL("DROP TABLE `transfer_table`")
        db.execSQL("ALTER TABLE `new_transfer_table` RENAME TO `transfer_table`")

        db.execSQL("CREATE TABLE IF NOT EXISTS `protocol_level_token_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `token_id` TEXT NOT NULL, `name` TEXT, `decimals` INTEGER NOT NULL, `account_address` TEXT, `balance` TEXT NOT NULL, `metadata` TEXT, `added_at` INTEGER NOT NULL DEFAULT 0, `is_hidden` INTEGER NOT NULL DEFAULT 0, `is_newly_received` INTEGER NOT NULL DEFAULT 0, `is_in_allow_list` INTEGER DEFAULT null, `is_in_deny_list` INTEGER DEFAULT null)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `new_contract_token_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contract_index` TEXT NOT NULL, `contract_name` TEXT NOT NULL DEFAULT '', `token_id` TEXT NOT NULL, `account_address` TEXT, `metadata` TEXT, `is_newly_received` INTEGER NOT NULL DEFAULT 0, `added_at` INTEGER NOT NULL DEFAULT 0)")
        db.execSQL(
            "INSERT INTO `new_contract_token_table` (`id`, `contract_index`, " +
                    "`contract_name`, `token_id`, `account_address`, `metadata`, " +
                    "`is_newly_received`, `added_at`) " +
                    "SELECT `id`, `contract_index`, " +
                    "`contract_name`, `token_id`, `account_address`, `token_metadata` as `metadata`, " +
                    "`is_newly_received`, 946684800000 + `id` * 60000 as `added_at` " +
                    "FROM `contract_token_table`"
        )
        db.execSQL("DROP TABLE `contract_token_table`")
        db.execSQL("ALTER TABLE `new_contract_token_table` RENAME TO `contract_token_table`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contract_token_table_contract_index_token_id_account_address` ON `contract_token_table` (`contract_index`, `token_id`, `account_address`)")
    }
}
