package com.concordium.wallet.data.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_recipient_table (
                address TEXT PRIMARY KEY NOT NULL,
                added_at INTEGER NOT NULL,
                name TEXT
            );
            """.trimIndent()
        )
    }
}
