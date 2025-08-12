package com.concordium.wallet.data.util

import com.concordium.sdk.serializing.CborMapper
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOrigin
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.model.TransactionSource
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Transfer
import com.reown.util.hexToBytes
import java.util.Date

fun RemoteTransaction.toTransaction() = Transaction(
    source = TransactionSource.Remote,
    timeStamp = Date(blockTime.toLong() * 1000),
    type = details.type,
    subtotal = subtotal,
    cost = cost,
    total = total,
    status = TransactionStatus.FINALIZED,
    outcome = details.outcome,
    blockHashes = arrayListOf(blockHash),
    hash = transactionHash,
    rejectReason = details.rejectReason,
    events = details.events,
    fromAddress = details.transferSource,
    toAddress = details.transferDestination,
    origin = origin,
    memoText = runCatching {
        CborMapper.INSTANCE.readValue(
            details.memo!!.hexToBytes(),
            String::class.java,
        )
    }.getOrNull(),
    tokenTransferAmount = details.tokenTransferAmount,
    tokenSymbol = details.tokenId,
)

fun Transfer.toTransaction() = Transaction(
    source = TransactionSource.Local,
    timeStamp = Date(createdAt),
    type = transactionType,
    subtotal = -amount,
    cost = cost,
    total = -(amount + cost),
    status = transactionStatus,
    outcome = outcome,
    blockHashes = null,
    hash = submissionId,
    rejectReason = null,
    events = null,
    fromAddress = fromAddress,
    toAddress = toAddress,
    origin = TransactionOrigin(TransactionOriginType.Self, null),
    memoText = runCatching {
        CborMapper.INSTANCE.readValue(
            memo!!.hexToBytes(),
            String::class.java,
        )
    }.getOrNull(),
    tokenTransferAmount = tokenTransferAmount,
    tokenSymbol = tokenSymbol,
)
