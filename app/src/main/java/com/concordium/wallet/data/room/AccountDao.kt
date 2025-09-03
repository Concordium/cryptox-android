package com.concordium.wallet.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.concordium.wallet.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT COUNT(id) FROM account_table")
    suspend fun getCount(): Int

    @Query("SELECT * FROM account_table ORDER BY read_only ASC, id DESC")
    fun getAllAsLiveData(): LiveData<List<Account>>

    @Transaction
    @Query("SELECT * FROM account_table ORDER BY read_only ASC, id DESC")
    fun getAllWithIdentityAsLiveData(): LiveData<List<AccountWithIdentity>>

    @Transaction
    @Query("SELECT * FROM account_table WHERE identity_id = :id ORDER BY read_only ASC, id DESC")
    fun getAllByIdentityIdWithIdentityAsLiveData(id: Int): LiveData<List<AccountWithIdentity>>

    @Transaction
    @Query("SELECT * FROM account_table WHERE id = :id")
    fun getByIdWithIdentityAsLiveData(id: Int): LiveData<AccountWithIdentity>

    @Query("SELECT * FROM account_table ORDER BY id DESC")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM account_table WHERE finalized_encrypted_balance IS NOT NULL")
    suspend fun getAllDone(): List<Account>

    @Transaction
    @Query("SELECT * FROM account_table WHERE finalized_encrypted_balance IS NOT NULL ORDER BY id DESC")
    suspend fun getAllDoneWithIdentity(): List<AccountWithIdentity>

    @Query("SELECT count(*) FROM account_table WHERE transaction_status != :status")
    suspend fun getStatusCount(status: Int): Int

    @Query("SELECT * FROM account_table WHERE identity_id = :id ORDER BY id ASC")
    suspend fun getAllByIdentityId(id: Int): List<Account>

    @Query("SELECT * FROM account_table WHERE id = :id")
    suspend fun findById(id: Int): Account?

    @Query("SELECT * FROM account_table WHERE address = :address")
    suspend fun findByAddress(address: String): Account?

    @Query("SELECT * FROM account_table WHERE is_active = 1")
    suspend fun getActive(): Account?

    @Query("SELECT * FROM account_table WHERE is_active = 1 LIMIT 1")
    fun getActiveFlow(): Flow<Account?>

    @Query("UPDATE account_table SET is_active = CASE WHEN address = :address THEN 1 ELSE 0 END")
    suspend fun activate(address: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg account: Account): List<Long>

    @Transaction
    suspend fun insertAndActivate(account: Account): Long {
        val id = insert(account).first()
        activate(account.address)
        return id
    }

    @Update
    suspend fun updateIdentity(vararg identity: Identity)

    @Query("SELECT * FROM identity_table WHERE id = :id")
    suspend fun findIdentityById(id: Int): Identity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg account: Account)

    @Transaction
    suspend fun updateExceptFinalState(vararg accounts: Account) {
        for (account in accounts) {
            // finalized state is final and cannot be changed
            val accountFromDB = findById(account.id)
            if (accountFromDB != null && accountFromDB.transactionStatus == TransactionStatus.FINALIZED) {
                account.transactionStatus = TransactionStatus.FINALIZED
            }
            update(account)
        }
    }

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM account_table")
    suspend fun deleteAll()
}
