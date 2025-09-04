package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.ContractTokenMetadata
import com.concordium.wallet.data.model.ProtocolLevelTokenMetadata
import com.concordium.wallet.data.room.typeconverter.ProtocolLevelTokenTypeConverters
import java.io.Serializable
import java.math.BigInteger

@Entity(tableName = "protocol_level_token_table")
@TypeConverters(ProtocolLevelTokenTypeConverters::class)
data class ProtocolLevelTokenEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "token_id")
    val tokenId: String,
    @ColumnInfo("name")
    val name: String?,
    @ColumnInfo(name = "decimals")
    val decimals: Int,
    @ColumnInfo(name = "account_address")
    val accountAddress: String?,
    @ColumnInfo(name = "balance")
    var balance: BigInteger = BigInteger.ZERO,
    @ColumnInfo(name = "metadata")
    val metadata: ProtocolLevelTokenMetadata?,
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
