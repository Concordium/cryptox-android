package com.concordium.wallet.data.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.concordium.wallet.data.room.WalletDatabase.Companion.VERSION_NUMBER
import com.concordium.wallet.data.room.migrations.MIGRATION_3_4
import com.concordium.wallet.data.room.migrations.MIGRATION_4_5
import com.concordium.wallet.data.room.migrations.MIGRATION_5_6
import com.concordium.wallet.data.room.migrations.MIGRATION_7_8
import com.concordium.wallet.data.room.migrations.MIGRATION_8_9
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
public abstract class WalletDatabase : RoomDatabase() {

    abstract fun identityDao(): IdentityDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun recipientDao(): RecipientDao
    abstract fun encryptedAmountDao(): EncryptedAmountDao
    abstract fun contractTokenDao(): ContractTokenDao

    companion object {

        const val VERSION_NUMBER = 9

        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        fun getDatabase(context: Context): WalletDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "wallet_database"
                )
                    .fallbackToDestructiveMigration()
                    // See auto migrations in the @Database declaration.
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
