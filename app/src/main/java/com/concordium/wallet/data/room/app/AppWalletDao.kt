package com.concordium.wallet.data.room.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AppWalletDao {
    @Query("SELECT * FROM wallets ORDER BY created_at ASC")
    abstract suspend fun getAll(): List<AppWalletEntity>

    @Query("SELECT * FROM WALLETS WHERE is_active=1")
    abstract suspend fun getActive(): AppWalletEntity

    @Query("UPDATE wallets SET is_active = CASE WHEN id=:walletId THEN 1 ELSE 0 END")
    abstract suspend fun activate(walletId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(wallet: AppWalletEntity)

    @Transaction
    open suspend fun insertAndActivate(wallet: AppWalletEntity) {
        insert(wallet)
        activate(wallet.id)
    }

    @Query("DELETE FROM wallets WHERE id=:walletId")
    protected abstract suspend fun delete(walletId: String)

    @Transaction
    open suspend fun deleteAndActivateAnother(
        walletToDeleteId: String,
        walletToActivateId: String
    ) {
        delete(walletToDeleteId)
        activate(walletToActivateId)
    }
}
