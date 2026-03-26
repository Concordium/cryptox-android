package com.concordium.wallet.data.backend.repository

import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendCallback
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.IdentityProvider

class IdentityProviderRepository {

    private val backend = App.appCore.session.backends.proxy

    fun getIdentityProviderInfo(
        success: (ArrayList<IdentityProvider>) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<ArrayList<IdentityProvider>> {
        val call = backend.getV2IdentityProviderInfo()
        backend.getV2IdentityProviderInfo().enqueue(object : BackendCallback<ArrayList<IdentityProvider>>() {

            override fun onResponseData(response: ArrayList<IdentityProvider>) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getGlobalInfoSuspended() = backend.getGlobalInfoSuspended()

    fun getIGlobalInfo(
        success: (GlobalParamsWrapper) -> Unit,
        failure: ((Throwable) -> Unit)?
    ): BackendRequest<GlobalParamsWrapper> {
        val call = backend.getGlobalInfo()
        call.enqueue(object : BackendCallback<GlobalParamsWrapper>() {

            override fun onResponseData(response: GlobalParamsWrapper) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }
}
