package com.concordium.wallet.ui.walletconnect

import android.content.Context
import com.concordium.sdk.crypto.ed25519.ED25519SecretKey
import com.concordium.sdk.transactions.MessageSigningDigest
import com.concordium.sdk.types.AccountAddress
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.cryptolib.ParameterToJsonInput
import com.concordium.wallet.data.cryptolib.SignMessageOutput
import com.concordium.wallet.data.cryptolib.X0
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountDataKeys
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.walletconnect.SignMessageParams
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Error
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Event
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.prettyPrint
import com.concordium.wallet.util.toHex
import com.reown.util.hexToBytes

class WalletConnectSignMessageRequestHandler(
    private val respondSuccess: (result: String) -> Unit,
    private val respondError: (message: String) -> Unit,
    private val emitEvent: (event: Event) -> Unit,
    private val emitState: (state: State) -> Unit,
    private val onFinish: () -> Unit,
    private val context: Context,
) {
    private lateinit var account: Account
    private lateinit var signMessageParams: SignMessageParams

    suspend fun start(
        params: String,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) = try {
        val signMessageParams = SignMessageParams.fromSessionRequestParams(
            if (params.startsWith('{'))
                params
            else
                "{$params}"
        )

        this.account = account
        this.signMessageParams = signMessageParams

        val reviewState = when (signMessageParams) {
            is SignMessageParams.Binary ->
                State.SessionRequestReview.SignRequestReview(
                    message = App.appCore.cryptoLibrary
                        .parameterToJson(
                            ParameterToJsonInput(
                                parameter = signMessageParams.data,
                                schema = signMessageParams.schema,
                                schemaVersion = null,
                            )
                        )
                        ?.prettyPrint()
                        ?: error("Failed to deserialize the raw binary data"),
                    canShowDetails = true,
                    account = account,
                    appMetadata = appMetadata,
                )

            is SignMessageParams.Text ->
                State.SessionRequestReview.SignRequestReview(
                    message = signMessageParams.data,
                    canShowDetails = false,
                    account = account,
                    appMetadata = appMetadata,
                )
        }

        emitState(reviewState)
    } catch (error: Exception) {
        Log.e("Failed to parse sign message parameters: $params", error)

        respondError("Failed parse the request: $error")

        emitEvent(
            Event.ShowFloatingError(
                Error.InvalidRequest
            )
        )

        onFinish()
    }

    fun onAuthorizedForApproval(
        accountKeys: AccountData,
    ) = try {
        val signKeyHex =
            App.appCore.gson.fromJson(accountKeys.keys.json, AccountDataKeys::class.java)
                .level0
                .keys
                .keys
                .signKey

        val message = when (val signMessageParams = this.signMessageParams) {
            is SignMessageParams.Binary ->
                signMessageParams.data.hexToBytes()

            is SignMessageParams.Text ->
                signMessageParams.data.encodeToByteArray()
        }

        val input = MessageSigningDigest.from(
            AccountAddress.from(account.address),
            message
        )

        val signatureHex = ED25519SecretKey.from(signKeyHex)
            .sign(input)
            .toHex()

        respondSuccess(App.appCore.gson.toJson(SignMessageOutput(X0(signatureHex))))
    } catch (e: Exception) {
        Log.e("failed_signing_message", e)

        respondError("Failed signing message: $e")

        emitEvent(
            Event.ShowFloatingError(
                Error.CryptographyFailed
            )
        )
    } finally {
        onFinish()
    }

    fun showReviewingMessageDetails() {
        val signMessageParams = this.signMessageParams
        if (signMessageParams is SignMessageParams.Binary) {
            emitEvent(
                Event.ShowDetailsDialog(
                    title = null,
                    prettyPrintDetails = context.getString(
                        R.string.wallet_connect_template_signature_request_details,
                        signMessageParams.data,
                    ),
                )
            )
        } else {
            Log.w("Nothing to show as details for ${signMessageParams::class.simpleName}")
        }
    }
}
