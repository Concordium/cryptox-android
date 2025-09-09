package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.ProtocolLevelToken
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
    @ColumnInfo(name = "is_paused", defaultValue = "0")
    val isPaused: Boolean = false,
) : Serializable {

    constructor(token: ProtocolLevelToken) : this(
        tokenId = token.tokenId,
        name = token.name,
        decimals = token.decimals,
        accountAddress = token.accountAddress,
        balance = token.balance,
        metadata = token.metadata,
        addedAt = token.addedAt,
        isHidden = token.isHidden,
        isNewlyReceived = token.isNewlyReceived,
        isInAllowList = token.isInAllowList,
        isInDenyList = token.isInDenyList,
        isPaused = token.isPaused,
    )

    fun toProtocolLevelToken(
        isSelected: Boolean = true,
    ) = ProtocolLevelToken(
        balance = balance,
        name = name,
        decimals = decimals,
        accountAddress = accountAddress ?: "",
        isNewlyReceived = isNewlyReceived,
        addedAt = addedAt,
        isSelected = isSelected,
        tokenId = tokenId,
        isHidden = isHidden,
        isInAllowList = isInAllowList,
        isInDenyList = isInDenyList,
        isPaused = isPaused,
    )
}
