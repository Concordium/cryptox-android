package com.concordium.wallet.data.room

import android.content.Context
import androidx.collection.LruCache
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.concordium.wallet.core.AppCore
import com.concordium.wallet.data.room.WalletDatabase.Companion.VERSION_NUMBER
import com.concordium.wallet.data.room.migrations.MIGRATION_10_11
import com.concordium.wallet.data.room.migrations.MIGRATION_3_4
import com.concordium.wallet.data.room.migrations.MIGRATION_4_5
import com.concordium.wallet.data.room.migrations.MIGRATION_5_6
import com.concordium.wallet.data.room.migrations.MIGRATION_7_8
import com.concordium.wallet.data.room.migrations.MIGRATION_8_9
import com.concordium.wallet.data.room.migrations.MIGRATION_9_10
import com.concordium.wallet.data.room.typeconverter.GlobalTypeConverters

@Database(
    entities = [
        Identity::class,
        Account::class,
        Transfer::class,
        Recipient::class,
        EncryptedAmount::class,
        ContractToken::class,
    ],
    version = VERSION_NUMBER,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
    ],
)
@TypeConverters(GlobalTypeConverters::class)
abstract class WalletDatabase : RoomDatabase() {

    abstract fun identityDao(): IdentityDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun recipientDao(): RecipientDao
    abstract fun encryptedAmountDao(): EncryptedAmountDao
    abstract fun contractTokenDao(): ContractTokenDao

    companion object {
        const val VERSION_NUMBER = 11
        private val instances = object : LruCache<String, WalletDatabase>(2) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: WalletDatabase,
                newValue: WalletDatabase?,
            ) {
                oldValue.close()
            }
        }

        @Deprecated(
            message = "Do not construct instances on your own",
            replaceWith = ReplaceWith(
                expression = "App.appCore.session.walletStorage.database",
                imports = arrayOf("com.concordium.wallet.App"),
            )
        )
        fun getDatabase(
            context: Context,
            fileNameSuffix: String = "",
        ): WalletDatabase = synchronized(this) {
            val name = "wallet_database$fileNameSuffix"

            instances[name]
                ?: Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "wallet_database$fileNameSuffix"
                )
                    .fallbackToDestructiveMigration()
                    // See auto migrations in the @Database declaration.
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10(
                            context = context,
                            gson = AppCore.getGson(),
                        ),
                        MIGRATION_10_11
                    )
                    .build()
                    .also { instances.put(name, it) }
        }
    }
}
