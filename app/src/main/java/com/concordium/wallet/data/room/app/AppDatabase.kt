package com.concordium.wallet.data.room.app

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppWalletEntity::class,
        AppNetworkEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appWalletDao(): AppWalletDao
    abstract fun appNetworkDao(): AppNetworkDao

    companion object {
        private const val FILE_NAME = "app_database"
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = synchronized(this) {
            instance
                ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    FILE_NAME,
                )
                    .build()
                    .also(::instance::set)
        }
    }
}
