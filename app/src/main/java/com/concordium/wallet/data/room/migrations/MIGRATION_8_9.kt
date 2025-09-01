package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `new_account_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `identity_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `submission_id` TEXT NOT NULL, `transaction_status` INTEGER NOT NULL, `encrypted_account_data` TEXT NOT NULL, `credential` TEXT, `cred_number` INTEGER NOT NULL, `revealed_attributes` TEXT NOT NULL, `finalized_balance` TEXT NOT NULL, `balance_at_disposal` TEXT NOT NULL, `total_shielded_balance` TEXT NOT NULL, `finalized_encrypted_balance` TEXT, `current_balance_status` INTEGER NOT NULL, `read_only` INTEGER NOT NULL, `finalized_account_release_schedule` TEXT, `cooldowns` TEXT NOT NULL, `account_delegation` TEXT, `account_baker` TEXT, `accountIndex` INTEGER)")
        db.execSQL("INSERT INTO `new_account_table` (`id`, `identity_id`, `name`, `address`, `submission_id`, `transaction_status`, `encrypted_account_data`, `credential`, `cred_number`, `revealed_attributes`, `finalized_balance`, `balance_at_disposal`, `total_shielded_balance`, `finalized_encrypted_balance`, `current_balance_status`, `read_only`, `finalized_account_release_schedule`, `cooldowns`, `account_delegation`, `account_baker`, `accountIndex`) SELECT `id`, `identity_id`, `name`, `address`, `submission_id`, `transaction_status`, `encrypted_account_data`, `credential`, `cred_number`, `revealed_attributes`, `finalized_balance`, 0, `total_shielded_balance`, `finalized_encrypted_balance`, `current_balance_status`, `read_only`, `finalized_account_release_schedule`, '[]', `account_delegation`, `account_baker`, `accountIndex` FROM `account_table`")
        db.execSQL("DROP TABLE `account_table`")
        db.execSQL("ALTER TABLE `new_account_table` RENAME TO `account_table`")

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_table_address` ON `account_table` (`address`)")
    }
}
