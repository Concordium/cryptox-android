package com.concordium.wallet.util

import java.net.URI

object IpfsGatewayFallback {

    private val gateways = listOf(
        "https://ipfs.filebase.io",
        "https://gateway.pinata.cloud",
        "https://ipfs.orbitor.dev",
    )

    fun getUrls(originalUrl: String): List<String> {
        val uri = try {
            URI(originalUrl)
        } catch (_: Exception) {
            return listOf(originalUrl)
        }

        if (!uri.scheme.equals("https", ignoreCase = true) ||
            uri.port != -1 ||
            uri.rawUserInfo != null ||
            uri.rawQuery != null ||
            uri.rawFragment != null
        ) {
            return listOf(originalUrl)
        }

        val host = uri.host ?: return listOf(originalUrl)

        val cid: String
        val suffix: String

        if (host.equals("ipfs.io", ignoreCase = true)) {
            val match = Regex(
                "^/ipfs/([A-Za-z0-9]+)(/.*)?$"
            ).matchEntire(uri.rawPath ?: "")
                ?: return listOf(originalUrl)

            cid = match.groupValues[1]
            suffix = match.groupValues[2]

        } else if (host.endsWith(
                ".ipfs.nftstorage.link",
                ignoreCase = true
            )
        ) {
            cid = host.substringBefore(".ipfs.", missingDelimiterValue = "")
            if (!Regex("^[A-Za-z0-9]+$").matches(cid)) {
                return listOf(originalUrl)
            }

            suffix = uri.rawPath ?: ""

        } else {
            return listOf(originalUrl)
        }

        return listOf(originalUrl) +
            gateways.map { gateway ->
                "$gateway/ipfs/$cid$suffix"
            }
    }
}
