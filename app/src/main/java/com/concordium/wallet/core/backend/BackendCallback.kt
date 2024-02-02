package com.concordium.wallet.core.backend

import com.concordium.wallet.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class BackendCallback<T> : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {

        Log.i("BackendCallback#onResponse")
        val isValid = isValidResponse(response)
        Log.i("BackendCallback#onResponse isValid: $isValid")
        if (isValid) {
            val body = response.body()
            if (body != null) {
                onResponseData(body)
            }
        } else {
            Log.i("BackendCallback#onResponse Response not valid")
            // Parse errorBody to error object
            try {
                val error = ErrorParser.parseError(response)
                Log.i("BackendCallback#onResponse Error: ${response.code()}, ${response.message()}, ${response.errorBody()?.string()}")
                if (error != null) {
                    Log.i("BackendCallback#onResponse $error")
                    onFailure(BackendErrorException(error))
                    return
                }
            } catch (e: Exception) {
                Log.e("BackendCallback#onResponse Response is not valid - error parsing failed. ${e.message}")
                onFailure(Exception("Response is not valid - error parsing failed", e))
                return
            }
            Log.e("BackendCallback#onResponse Response is not valid - error parsing failed")
            onFailure(Exception("Response is not valid - error parsing failed"))
        }
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        Log.e("BackendCallback#onFailure Response is not valid - error parsing failed")
        onFailure(t)
    }

    protected abstract fun onResponseData(response: T)

    protected abstract fun onFailure(t: Throwable)

    private fun isValidResponse(response: Response<T>): Boolean {
        val responseBody = response.body()
        if (responseBody != null && response.isSuccessful) {
            return true
        }
        return false
    }
}