package com.concordium.wallet.util

import org.junit.Assert.assertEquals
import org.junit.Test

class IpfsGatewayFallbackTest {

    private val cid = "QmYwAPJzv5CZsnAzt8auVTL"

    private val gateways = listOf(
        "https://ipfs.filebase.io",
        "https://gateway.pinata.cloud",
        "https://ipfs.orbitor.dev",
    )

    private fun expectedUrls(
        original: String,
        suffix: String = "",
    ): List<String> =
        listOf(original) +
            gateways.map { "$it/ipfs/$cid$suffix" }

    @Test
    fun `path based IPFS URL generates fallbacks in order`() {
        val original = "https://ipfs.io/ipfs/$cid"

        assertEquals(
            expectedUrls(original),
            IpfsGatewayFallback.getUrls(original),
        )
    }

    @Test
    fun `subdomain based IPFS URL generates fallbacks`() {
        val original = "https://$cid.ipfs.nftstorage.link/"

        assertEquals(
            expectedUrls(original, "/"),
            IpfsGatewayFallback.getUrls(original),
        )
    }

    @Test
    fun `path based IPFS URL preserves nested path`() {
        val original = "https://ipfs.io/ipfs/$cid/images/icon.png"

        assertEquals(
            expectedUrls(original, "/images/icon.png"),
            IpfsGatewayFallback.getUrls(original),
        )
    }

    @Test
    fun `subdomain based IPFS URL preserves nested path`() {
        val original =
            "https://$cid.ipfs.nftstorage.link/metadata/token.json"

        assertEquals(
            expectedUrls(original, "/metadata/token.json"),
            IpfsGatewayFallback.getUrls(original),
        )
    }

    @Test
    fun `ordinary HTTPS URLs remain unchanged`() {
        val urls = listOf(
            "https://example.com/metadata.json",
            "https://api.bankofmemories.org/bridge/metadata-token",
            "https://euroeccdmetadataprod.blob.core.windows.net/metadata.json",
        )

        urls.forEach { url ->
            assertEquals(
                listOf(url),
                IpfsGatewayFallback.getUrls(url),
            )
        }
    }

    @Test
    fun `unsupported URLs remain unchanged`() {
        val urls = listOf(
            "http://ipfs.io/ipfs/$cid",
            "https://ipfs.io:8443/ipfs/$cid",
            "https://user@ipfs.io/ipfs/$cid",
            "https://ipfs.io/ipfs/$cid?download=1",
            "https://ipfs.io/ipfs/$cid#fragment",
            "https://ipfs.io/not-ipfs/$cid",
            "https://ipfs.io/ipfs/",
            "not a valid URL",
        )

        urls.forEach { url ->
            assertEquals(
                listOf(url),
                IpfsGatewayFallback.getUrls(url),
            )
        }
    }

    @Test
    fun `lookalike domains are not rewritten`() {
        val urls = listOf(
            "https://ipfs.io.evil.com/ipfs/$cid",
            "https://fakeipfs.io/ipfs/$cid",
            "https://$cid.ipfs.nftstorage.link.evil.com/",
            "https://evil-nftstorage.link/ipfs/$cid",
        )

        urls.forEach { url ->
            assertEquals(
                listOf(url),
                IpfsGatewayFallback.getUrls(url),
            )
        }
    }

    @Test
    fun `uppercase gateway hostname is recognized`() {
        val original = "https://IPFS.IO/ipfs/$cid"

        assertEquals(
            expectedUrls(original),
            IpfsGatewayFallback.getUrls(original),
        )
    }

    @Test
    fun `different IPFS CIDs are preserved`() {
        val cidV1 =
            "bafkreidpmermpznx6abilejdvbnpai6gcpamszarrg3thdlgpuxk5gn3nq"

        val original = "https://ipfs.io/ipfs/$cidV1"

        assertEquals(
            listOf(original) +
                gateways.map { "$it/ipfs/$cidV1" },
            IpfsGatewayFallback.getUrls(original),
        )
    }
}
