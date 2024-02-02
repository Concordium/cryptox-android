package com.concordium.wallet.util

class Event<T>(private val content: T) {
    private var used: Boolean = false

    val contentOrNullIfUsed: T?
        get() {
            if (used) {
                return null
            }
            used = true
            return content
        }

    fun wasUsed(): Boolean {
        return used
    }
}