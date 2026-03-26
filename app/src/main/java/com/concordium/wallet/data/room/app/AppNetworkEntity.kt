package com.concordium.wallet.data.room.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.concordium.wallet.core.multinetwork.AppNetwork
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Date

@Entity(
    tableName = "networks",
    indices = [Index(value = ["name"], unique = true)]
)
data class AppNetworkEntity(
    @PrimaryKey
    @ColumnInfo(name = "genesis_hash")
    val genesisHash: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "wallet_proxy_url")
    val walletProxyUrl: String,
    @ColumnInfo(name = "ccdscan_frontend_url")
    val ccdScanFrontendUrl: String?,
    @ColumnInfo(name = "ccdscan_backend_url")
    val ccdScanBackendUrl: String?,
    @ColumnInfo(name = "notifications_service_url")
    val notificationsServiceUrl: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
) {
    constructor(
        network: AppNetwork,
        isActive: Boolean,
    ) : this(
        genesisHash = network.genesisHash,
        name = network.name,
        walletProxyUrl = network.walletProxyUrl.toString(),
        ccdScanFrontendUrl = network.ccdScanFrontendUrl?.toString(),
        ccdScanBackendUrl = network.ccdScanBackendUrl?.toString(),
        notificationsServiceUrl = network.notificationsServiceUrl?.toString(),
        createdAt = network.createdAt.time,
        isActive = isActive,
    )

    fun toNetwork(): AppNetwork = AppNetwork(
        genesisHash = genesisHash,
        name = name,
        createdAt = Date(createdAt),
        walletProxyUrl = walletProxyUrl.toHttpUrl(),
        ccdScanFrontendUrl = ccdScanFrontendUrl?.toHttpUrl(),
        ccdScanBackendUrl = ccdScanBackendUrl?.toHttpUrl(),
        notificationsServiceUrl = notificationsServiceUrl?.toHttpUrl(),
    )
}
