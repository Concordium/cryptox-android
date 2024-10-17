package com.concordium.wallet.data.room.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppWalletEntity::class,
    ],
    version = 1,
    exportSchema = false, // TODO enable later
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appWalletDao(): AppWalletDao

    companion object {
        const val FILE_NAME = "app_database"

        fun getDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                FILE_NAME,
            )
                .build()
    }
}
