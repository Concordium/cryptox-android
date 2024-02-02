package com.concordium.wallet.extension

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

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

fun <T> LiveData<T>.observe(owner: LifecycleOwner, action: (T) -> Unit) {
    observe(owner, Observer { action(it) })
}