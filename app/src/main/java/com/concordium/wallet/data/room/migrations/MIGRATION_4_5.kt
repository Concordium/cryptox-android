package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE account_table ADD `account_delegation` TEXT")
        db.execSQL("ALTER TABLE account_table ADD `account_baker` TEXT")
    }
}
