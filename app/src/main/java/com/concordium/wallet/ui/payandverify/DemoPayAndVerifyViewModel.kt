package com.concordium.wallet.ui.payandverify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectWalletDelegate
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient

class DemoPayAndVerifyViewModel
private constructor(
    application: Application,
    private val defaultWalletConnectWalletDelegate: SignClient.WalletDelegate,
) : AndroidViewModel(application) {

    /**
     * A constructor for Android.
     */
    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        defaultWalletConnectWalletDelegate = LoggingWalletConnectWalletDelegate(),
    )

    fun initialize(
        accountAddress: String,
    ) {
        SignClient.setWalletDelegate(object : SignClient.WalletDelegate {
            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
                defaultWalletConnectWalletDelegate.onConnectionStateChange(state)
            }

            override fun onError(error: Sign.Model.Error) {
                defaultWalletConnectWalletDelegate.onError(error)
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
                defaultWalletConnectWalletDelegate.onSessionDelete(deletedSession)
            }

            override fun onSessionExtend(session: Sign.Model.Session) {
                defaultWalletConnectWalletDelegate.onSessionExtend(session)
            }

            override fun onSessionProposal(
                sessionProposal: Sign.Model.SessionProposal,
                verifyContext: Sign.Model.VerifyContext,
            ) {
                defaultWalletConnectWalletDelegate.onSessionProposal(sessionProposal, verifyContext)
                // TODO auto approve
            }

            override fun onSessionRequest(
                sessionRequest: Sign.Model.SessionRequest,
                verifyContext: Sign.Model.VerifyContext,
            ) {
                defaultWalletConnectWalletDelegate.onSessionRequest(sessionRequest, verifyContext)
            }

            override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
                defaultWalletConnectWalletDelegate.onSessionSettleResponse(settleSessionResponse)
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
                defaultWalletConnectWalletDelegate.onSessionUpdateResponse(sessionUpdateResponse)
            }
        })
    }

    override fun onCleared() {
        SignClient.setWalletDelegate(defaultWalletConnectWalletDelegate)
    }
}
