package com.concordium.wallet.data.room.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.SparseIntArray
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.concordium.wallet.data.room.typeconverter.IdentityTypeConverters

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `identity_table` ADD COLUMN `identity_provider_id` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `identity_table` ADD COLUMN `identity_index` INTEGER NOT NULL DEFAULT 0")

        // Rewrite identity indices and fill provider IDs to achieve uniqueness.
        // Identity index is an incrementing counter separate for each identity provider.
        val identityTypeConverters = IdentityTypeConverters()
        database.query("SELECT `id`, `identity_provider` FROM `identity_table`")
            .use { identitiesCursor ->
                val indicesByProviderId = SparseIntArray()
                while (identitiesCursor.moveToNext()) {
                    val identityId = identitiesCursor.getInt(0)
                    val identityProvider =
                        identityTypeConverters.jsonToIdentityProvider(identitiesCursor.getString(1))
                    val identityProviderId = identityProvider.ipInfo.ipIdentity

                    val indexToSet = indicesByProviderId.get(identityProviderId, -1) + 1
                    indicesByProviderId.put(identityProviderId, indexToSet)

                    database.update(
                        "identity_table",
                        SQLiteDatabase.CONFLICT_FAIL,
                        ContentValues(1).apply {
                            put("identity_index", indexToSet)
                            put("identity_provider_id", identityProviderId)
                        },
                        "`id`=?",
                        arrayOf(identityId)
                    )
                }
            }

        database.execSQL("CREATE TABLE IF NOT EXISTS `account_contract_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account_address` TEXT NOT NULL, `contract_index` TEXT NOT NULL)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_contract_table_account_address_contract_index` ON `account_contract_table` (`account_address`, `contract_index`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `contract_token_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contract_index` TEXT NOT NULL, `token_id` TEXT NOT NULL, `account_address` TEXT, `is_fungible` INTEGER NOT NULL, `token_metadata` TEXT, `contract_name` TEXT NOT NULL DEFAULT '')")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contract_token_table_contract_index_token_id_account_address` ON `contract_token_table` (`contract_index`, `token_id`, `account_address`)")

        database.execSQL("CREATE TABLE IF NOT EXISTS `_new_account_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `identity_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `submission_id` TEXT NOT NULL, `transaction_status` INTEGER NOT NULL, `encrypted_account_data` TEXT NOT NULL, `revealed_attributes` TEXT NOT NULL, `credential` TEXT, `finalized_balance` INTEGER NOT NULL, `current_balance` INTEGER NOT NULL, `total_balance` INTEGER NOT NULL, `total_unshielded_balance` INTEGER NOT NULL, `total_shielded_balance` INTEGER NOT NULL, `finalized_encrypted_balance` TEXT, `current_encrypted_balance` TEXT, `current_balance_status` INTEGER NOT NULL, `total_staked` INTEGER NOT NULL, `total_at_disposal` INTEGER NOT NULL, `read_only` INTEGER NOT NULL, `finalized_account_release_schedule` TEXT, `baker_id` INTEGER, `account_delegation` TEXT, `account_baker` TEXT, `accountIndex` INTEGER, `cred_number` INTEGER NOT NULL)")
        database.execSQL("INSERT INTO `_new_account_table` (`id`,`identity_id`,`name`,`address`,`submission_id`,`transaction_status`,`encrypted_account_data`,`revealed_attributes`,`credential`,`finalized_balance`,`current_balance`,`total_balance`,`total_unshielded_balance`,`total_shielded_balance`,`finalized_encrypted_balance`,`current_encrypted_balance`,`current_balance_status`,`total_staked`,`total_at_disposal`,`read_only`,`finalized_account_release_schedule`,`baker_id`,`account_delegation`,`account_baker`,`cred_number`) SELECT `id`,`identity_id`,`name`,`address`,`submission_id`,`transaction_status`,`encrypted_account_data`,`revealed_attributes`,`credential`,`finalized_balance`,`current_balance`,`total_balance`,`total_unshielded_balance`,`total_shielded_balance`,`finalized_encrypted_balance`,`current_encrypted_balance`,`current_balance_status`,`total_staked`,`total_at_disposal`,`read_only`,`finalized_account_release_schedule`,`baker_id`,`account_delegation`,`account_baker`,0 FROM `account_table`")
        database.execSQL("DROP TABLE `account_table`")
        database.execSQL("ALTER TABLE `_new_account_table` RENAME TO `account_table`")

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_table_address` ON `account_table` (`address`)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_identity_table_identity_provider_id_identity_index` ON `identity_table` (`identity_provider_id`, `identity_index`)")
    }
}