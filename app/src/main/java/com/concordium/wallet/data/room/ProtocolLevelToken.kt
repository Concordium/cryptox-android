package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.typeconverter.PLTTypeConverters
import java.io.Serializable
import java.math.BigInteger

@Entity(
    tableName = "protocol_level_token_table",
    indices = [Index(value = ["tokenId"], unique = true)]
)
@TypeConverters(PLTTypeConverters::class)
data class ProtocolLevelToken(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "tokenId")
    val tokenId: String,
    @ColumnInfo(name = "token_metadata")
    val tokenMetadata: TokenMetadata?,
    @ColumnInfo(name = "account_address")
    val accountAddress: String?,
    @ColumnInfo(name = "token_balance")
    var balance: BigInteger = BigInteger.ZERO,
    @ColumnInfo(name = "added_at", defaultValue = "0")
    var addedAt: Long = 0L,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    var isHidden: Boolean = false,
    @ColumnInfo(name = "is_newly_received", defaultValue = "0")
    val isNewlyReceived: Boolean,
    @ColumnInfo(name = "is_in_allow_list", defaultValue = "null")
    val isInAllowList: Boolean? = null,
    @ColumnInfo(name = "is_in_deny_list", defaultValue = "null")
    val isInDenyList: Boolean? = null,
) : Serializable
