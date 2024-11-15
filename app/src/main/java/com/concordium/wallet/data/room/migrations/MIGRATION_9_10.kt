package com.concordium.wallet.data.room.migrations

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.concordium.wallet.core.migration.TwoWalletsMigration
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.room.typeconverter.GlobalTypeConverters

fun MIGRATION_9_10(context: Context) = object : Migration(9, 10) {
    private val globalTypeConverters = GlobalTypeConverters()
    private val twoWalletsMigration = TwoWalletsMigration(context)

    override fun migrate(database: SupportSQLiteDatabase) = with(database) {
        execSQL("CREATE TABLE IF NOT EXISTS `_new_identity_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `status` TEXT NOT NULL, `detail` TEXT, `code_uri` TEXT NOT NULL, `next_account_number` INTEGER NOT NULL, `identity_provider` TEXT NOT NULL, `identity_object` TEXT, `private_id_object_data_encrypted` TEXT, `identity_provider_id` INTEGER NOT NULL, `identity_index` INTEGER NOT NULL)")
        execSQL("INSERT INTO `_new_identity_table` (`id`,`name`,`status`,`detail`,`code_uri`,`next_account_number`,`identity_provider`,`identity_object`,`private_id_object_data_encrypted`,`identity_provider_id`,`identity_index`) SELECT `id`,`name`,`status`,`detail`,`code_uri`,`next_account_number`,`identity_provider`,`identity_object`,`private_id_object_data_encrypted`,`identity_provider_id`,`identity_index` FROM `identity_table`")
        execSQL("DROP TABLE `identity_table`")
        execSQL("ALTER TABLE `_new_identity_table` RENAME TO `identity_table`")
        execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_identity_table_identity_provider_id_identity_index` ON `identity_table` (`identity_provider_id`, `identity_index`)")

        // Migrate identity encrypted data:
        // wrap the existing data into EncryptedData, or overwrite with null if missing.
        database.query("SELECT `id`, `private_id_object_data_encrypted` FROM `identity_table`")
            .use { identitiesCursor ->
                while (identitiesCursor.moveToNext()) {
                    val oldPrivateIdObjectDataEncrypted = identitiesCursor.getString(1)
                        ?.takeIf(String::isNotEmpty)
                    val identityId = identitiesCursor.getInt(0)
                    val migratedPrivateIdObjectDataEncrypted: EncryptedData? =
                        oldPrivateIdObjectDataEncrypted
                            ?.let(twoWalletsMigration::migrateOldEncryptedData)

                    update(
                        "identity_table",
                        SQLiteDatabase.CONFLICT_FAIL,
                        ContentValues(1).apply {
                            put(
                                "private_id_object_data_encrypted",
                                globalTypeConverters.encryptedDataToJson(
                                    migratedPrivateIdObjectDataEncrypted
                                )
                            )
                        },
                        "`id`=?",
                        arrayOf(identityId)
                    )
                }
            }

        execSQL("CREATE TABLE IF NOT EXISTS `_new_account_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `identity_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `submission_id` TEXT NOT NULL, `transaction_status` INTEGER NOT NULL, `encrypted_account_data` TEXT, `credential` TEXT, `cred_number` INTEGER NOT NULL, `revealed_attributes` TEXT NOT NULL, `finalized_balance` TEXT NOT NULL, `balance_at_disposal` TEXT NOT NULL, `total_shielded_balance` TEXT NOT NULL, `finalized_encrypted_balance` TEXT, `current_balance_status` INTEGER NOT NULL, `read_only` INTEGER NOT NULL, `finalized_account_release_schedule` TEXT, `cooldowns` TEXT NOT NULL, `account_delegation` TEXT, `account_baker` TEXT, `accountIndex` INTEGER)")
        execSQL("INSERT INTO `_new_account_table` (`id`,`identity_id`,`name`,`address`,`submission_id`,`transaction_status`,`encrypted_account_data`,`credential`,`cred_number`,`revealed_attributes`,`finalized_balance`,`balance_at_disposal`,`total_shielded_balance`,`finalized_encrypted_balance`,`current_balance_status`,`read_only`,`finalized_account_release_schedule`,`cooldowns`,`account_delegation`,`account_baker`,`accountIndex`) SELECT `id`,`identity_id`,`name`,`address`,`submission_id`,`transaction_status`,`encrypted_account_data`,`credential`,`cred_number`,`revealed_attributes`,`finalized_balance`,`balance_at_disposal`,`total_shielded_balance`,`finalized_encrypted_balance`,`current_balance_status`,`read_only`,`finalized_account_release_schedule`,`cooldowns`,`account_delegation`,`account_baker`,`accountIndex` FROM `account_table`")
        execSQL("DROP TABLE `account_table`")
        execSQL("ALTER TABLE `_new_account_table` RENAME TO `account_table`")
        execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_table_address` ON `account_table` (`address`)")

        // Migrate account encrypted data:
        // wrap the existing data into EncryptedData, or overwrite with null if missing.
        database.query("SELECT `id`, `encrypted_account_data` FROM `account_table`")
            .use { accountsCursor ->
                while (accountsCursor.moveToNext()) {
                    val oldEncryptedAccountData = accountsCursor.getString(1)
                        ?.takeIf(String::isNotEmpty)
                    val accountId = accountsCursor.getInt(0)
                    val migratedEncryptedAccountData: EncryptedData? =
                        oldEncryptedAccountData
                            ?.let(twoWalletsMigration::migrateOldEncryptedData)

                    update(
                        "account_table",
                        SQLiteDatabase.CONFLICT_FAIL,
                        ContentValues(1).apply {
                            put(
                                "encrypted_account_data",
                                globalTypeConverters.encryptedDataToJson(
                                    migratedEncryptedAccountData
                                )
                            )
                        },
                        "`id`=?",
                        arrayOf(accountId)
                    )
                }
            }
    }
}
