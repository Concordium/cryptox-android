package com.concordium.wallet.data.backend

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * A simple [CookieJar] which stores the cookies in the memory, without a persistence.
 */
class InMemoryCookieJar : CookieJar {
    private val cookieSet = mutableSetOf<StoredCookie>()

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(this) {
        cookieSet.removeAll(StoredCookie::isExpired)
        return@synchronized cookieSet.mapNotNull { it.takeIf { it.matches(url) }?.cookie }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this) {
            cookies.mapTo(cookieSet, ::StoredCookie)
        }
    }

    private class StoredCookie(val cookie: Cookie) {
        val isExpired: Boolean
            get() = cookie.expiresAt < System.currentTimeMillis()

        fun matches(url: HttpUrl) =
            cookie.matches(url)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StoredCookie) return false

            return this.cookie.name == other.cookie.name
                    && this.cookie.domain == other.cookie.domain
                    && this.cookie.secure == other.cookie.secure
                    && this.cookie.hostOnly == other.cookie.hostOnly
        }

        override fun hashCode(): Int = with(cookie) {
            var result = 17
            result = 31 * result + name.hashCode()
            result = 31 * result + domain.hashCode()
            result = 31 * result + path.hashCode()
            result = 31 * result + secure.hashCode()
            result = 31 * result + hostOnly.hashCode()
            return result
        }
    }
}
