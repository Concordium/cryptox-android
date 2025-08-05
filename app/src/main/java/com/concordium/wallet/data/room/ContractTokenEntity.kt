package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.typeconverter.ContractTypeConverters
import java.io.Serializable

@Entity(
    tableName = "contract_token_table",
    indices = [Index(value = ["contract_index", "token_id", "account_address"], unique = true)]
)
@TypeConverters(ContractTypeConverters::class)
data class ContractTokenEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "contract_index")
    val contractIndex: String,
    @ColumnInfo(name = "contract_name", defaultValue = "")
    val contractName: String,
    @ColumnInfo(name = "token_id")
    val token: String,
    @ColumnInfo(name = "account_address")
    val accountAddress: String?,
    @ColumnInfo(name = "is_fungible")
    val isFungible: Boolean,
    @ColumnInfo(name = "token_metadata")
    val tokenMetadata: TokenMetadata?,
    @ColumnInfo(name = "is_newly_received", defaultValue = "0")
    val isNewlyReceived: Boolean,
    @ColumnInfo(name = "added_at", defaultValue = "0")
    var addedAt: Long = 0L,
) : Serializable
