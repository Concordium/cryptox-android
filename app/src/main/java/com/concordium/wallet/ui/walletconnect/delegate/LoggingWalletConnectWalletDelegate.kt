package com.concordium.wallet.ui.walletconnect.delegate

import com.concordium.wallet.util.Log
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient

class LoggingWalletConnectWalletDelegate : SignClient.WalletDelegate {
    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        Log.d(
            "connection_state_changed:" +
                    "\nnewState=$state"
        )
    }

    override fun onError(error: Sign.Model.Error) {
        Log.e("general_error_occurred", error.throwable)
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        Log.d(
            "session_deleted:" +
                    "\ndeleted=$deletedSession"
        )
    }

    override fun onSessionExtend(session: Sign.Model.Session) {
        Log.d(
            "session_extended:" +
                    "\nextended=$session"
        )
    }

    override fun onSessionProposal(
        sessionProposal: Sign.Model.SessionProposal,
        verifyContext: Sign.Model.VerifyContext
    ) {
        Log.d(
            "received_session_proposal:" +
                    "\nproposal=$sessionProposal"
        )
    }

    override fun onSessionRequest(
        sessionRequest: Sign.Model.SessionRequest,
        verifyContext: Sign.Model.VerifyContext
    ) {
        Log.d(
            "received_session_request:" +
                    "\nrequestId=${sessionRequest.request.id}"
        )
    }

    override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
        Log.d(
            "received_session_settle_response:" +
                    "\nresponse=$settleSessionResponse"
        )
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
        Log.d(
            "received_session_update_response:" +
                    "\nresponse=$sessionUpdateResponse"
        )
    }
}
