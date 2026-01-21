package com.concordium.wallet.ui.walletconnect

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2.GONE
import androidx.viewpager2.widget.ViewPager2.INVISIBLE
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import androidx.viewpager2.widget.ViewPager2.VISIBLE
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWalletConnectAccountSelectionBinding
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofRequestReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectProgressBinding
import com.concordium.wallet.databinding.FragmentWalletConnectSessionProposalReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectSignMessageReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionRequestReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionSubmittedFragmentBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import java.math.BigInteger

/**
 * A view that handles WalletConnect with bottom sheet dialogs.
 *
 * @see WalletConnectViewModel
 */
class WalletConnectView(
    private val activity: BaseActivity,
    private val fragmentManager: FragmentManager,
    private val authDelegate: AuthDelegate,
    private val viewModel: WalletConnectViewModel,
) : LifecycleOwner by activity {

    fun init() {
        subscribeToState()
        subscribeToEvents()
    }

    private fun subscribeToState() {
        subscribeBottomSheetToState()

        // Show a notice when an action is required
        // but the parent screen is not visible.
        viewModel.stateFlow.collect(this) { state ->
            when (state) {
                is WalletConnectViewModel.State.SessionProposalReview,
                is WalletConnectViewModel.State.SessionRequestReview,
                -> {
                    val lifecycleState = lifecycle.currentState
                    if (lifecycleState >= Lifecycle.State.CREATED
                        && lifecycleState < Lifecycle.State.STARTED
                    ) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.wallet_connect_pending_request_notice),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                else -> {
                    // No notification needed.
                }
            }
        }
    }

    // When dealing with fragment transaction,
    // the Started state must be ensured to avoid state loss errors.
    private fun subscribeBottomSheetToState(
    ) = viewModel.stateFlow.collectWhenStarted(this) { state ->
        when (state) {
            WalletConnectViewModel.State.Idle -> {
                hideBottomSheet()
            }

            WalletConnectViewModel.State.WaitingForSessionProposal -> {
                showConnecting()
            }

            is WalletConnectViewModel.State.SessionProposalReview -> {
                showSessionProposalReview(
                    selectedAccount = state.selectedAccount,
                    appMetadata = state.appMetadata,
                )
            }

            is WalletConnectViewModel.State.AccountSelection -> {
                showAccountSelection(
                    accounts = state.accounts,
                )
            }

            is WalletConnectViewModel.State.IdentitySelection -> {
                showIdentitySelection(
                    identities = state.identities,
                )
            }

            WalletConnectViewModel.State.WaitingForSessionRequest -> {
                showConnecting()
            }

            is WalletConnectViewModel.State.SessionRequestReview.TransactionRequestReview -> {
                showTransactionRequestReview(
                    method = state.method,
                    receiver = state.receiver,
                    amount = state.amount,
                    token = state.token,
                    estimatedFee = state.estimatedFee,
                    canShowDetails = state.canShowDetails,
                    isEnoughFunds = state.isEnoughFunds,
                    sponsor = state.sponsor,
                    account = state.account,
                    appMetadata = state.appMetadata,
                )
            }

            is WalletConnectViewModel.State.SessionRequestReview.SignRequestReview -> {
                showSignRequestReview(
                    message = state.message,
                    canShowDetails = state.canShowDetails,
                    account = state.account,
                    appMetadata = state.appMetadata,
                )
            }

            is WalletConnectViewModel.State.SessionRequestReview.IdentityProofRequestReview -> {
                showIdentityProofRequestReview(
                    appMetadata = state.appMetadata,
                    claims = state.claims,
                    currentClaim = state.currentClaim,
                    provableState = state.provable
                )
            }

            is WalletConnectViewModel.State.TransactionSubmitted -> {
                showTransactionSubmitted(
                    submissionId = state.submissionId,
                    estimatedFee = state.estimatedFee,
                )
            }
        }
    }

    private fun subscribeToEvents() = viewModel.eventsFlow.collect(this) { event ->
        when (event) {
            WalletConnectViewModel.Event.ShowAuthentication -> {
                authDelegate.showAuthentication(activity) { password ->
                    viewModel.onAuthenticated(password)
                }
            }

            is WalletConnectViewModel.Event.ShowFloatingError -> {
                val errorRes = when (event.error) {
                    WalletConnectViewModel.Error.ConnectionFailed ->
                        R.string.wallet_connect_error_connection_failed

                    WalletConnectViewModel.Error.CryptographyFailed ->
                        R.string.wallet_connect_error_cryptography_failed

                    WalletConnectViewModel.Error.InvalidRequest ->
                        R.string.wallet_connect_error_invalid_request

                    WalletConnectViewModel.Error.InvalidLink ->
                        R.string.wallet_connect_error_invalid_link

                    WalletConnectViewModel.Error.LoadingFailed ->
                        R.string.wallet_connect_error_loading_failed

                    WalletConnectViewModel.Error.AccountNotFound ->
                        R.string.wallet_connect_error_account_not_found

                    WalletConnectViewModel.Error.NoAccounts ->
                        R.string.wallet_connect_error_no_accounts

                    WalletConnectViewModel.Error.ResponseFailed ->
                        R.string.wallet_connect_error_response_failed

                    WalletConnectViewModel.Error.TransactionSubmitFailed ->
                        R.string.wallet_connect_error_tx_submit_failed

                    WalletConnectViewModel.Error.NoSupportedChains ->
                        R.string.wallet_connect_error_no_supported_chains

                    WalletConnectViewModel.Error.UnsupportedMethod ->
                        R.string.wallet_connect_error_unsupported_methods

                    WalletConnectViewModel.Error.InternalError ->
                        R.string.wallet_connect_error_internal_error

                    WalletConnectViewModel.Error.NotSeedPhraseWalletError ->
                        R.string.wallet_connect_error_not_seed_phrase_wallet
                }

                val duration = when (event.error) {
                    WalletConnectViewModel.Error.ConnectionFailed,
                    WalletConnectViewModel.Error.AccountNotFound,
                    ->
                        Toast.LENGTH_LONG

                    else ->
                        Toast.LENGTH_SHORT
                }

                Toast.makeText(activity, errorRes, duration).show()
            }

            is WalletConnectViewModel.Event.ShowDetailsDialog -> {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(event.title)
                    .setMessage(event.prettyPrintDetails)
                    .setPositiveButton(R.string.dialog_ok, null)
                    .show()
                    .apply {
                        val messageTextView = findViewById<View>(android.R.id.message) as? TextView
                            ?: return@apply

                        with(messageTextView) {
                            typeface = Typeface.MONOSPACE
                            setTextIsSelectable(true)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        }
                    }
            }

            else -> {}
        }
    }

    private fun showSessionProposalReview(
        selectedAccount: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        getShownBottomSheet().showSessionProposalReview { (view, _) ->
            initSessionProposalReviewView(
                view = view,
                selectedAccount = selectedAccount,
                appMetadata = appMetadata
            )
        }
    }

    private fun initSessionProposalReviewView(
        view: FragmentWalletConnectSessionProposalReviewBinding,
        selectedAccount: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) = with(view) {
        Glide.with(appIconImageView.context)
            .load(appMetadata.iconUrl)
            .placeholder(ThemedCircularProgressDrawable(root.context))
            .fitCenter()
            .into(appIconImageView)

        titleTextView.text = root.context.getString(
            R.string.wallet_connect_template_session_proposal_review_title,
            appMetadata.name
        )

        appUrlTextView.text = appMetadata.url

        with(selectedAccountInclude) {
            accAddress.text = selectedAccount.getAccountName()
            accBalance.isVisible = false
            accIdentity.isVisible = true
            accIdentity.text = viewModel.getIdentityFromRepository(selectedAccount)?.name ?: ""
        }

        chooseAccountButton.setOnClickListener {
            viewModel.onChooseAccountClicked()
        }

        declineButton.setOnClickListener {
            viewModel.rejectSessionProposal()
        }

        allowButton.setOnClickListener {
            viewModel.approveSessionProposal()
        }
    }

    private fun showAccountSelection(
        accounts: List<Account>,
    ) {
        getShownBottomSheet().showAccountSelection { (view, _) ->
            initAccountSelectionView(
                view = view,
                accounts = accounts,
            )
        }
    }

    private fun initAccountSelectionView(
        view: FragmentWalletConnectAccountSelectionBinding,
        accounts: List<Account>,
    ) = with(view) {
        val adapter = ChooseAccountListAdapter(root.context, accounts)
        adapter.setChooseAccountClickListener(viewModel::onAccountSelected)
        accountsListView.adapter = adapter
    }

    private fun showIdentitySelection(
        identities: List<Identity>,
    ) {
        getShownBottomSheet().showIdentitySelection { (view, _) ->
            initIdentitySelectionView(
                view = view,
                identities = identities,
            )
        }
    }

    private fun initIdentitySelectionView(
        view: FragmentWalletConnectAccountSelectionBinding,
        identities: List<Identity>,
    ) = with(view) {
        titleTextView.setText(R.string.wallet_connect_choose_another_identity)
        val adapter = ChooseIdentityListAdapter(root.context, identities)
        adapter.setOnClickListener(viewModel::onIdentitySelected)
        accountsListView.adapter = adapter
    }

    private fun showTransactionRequestReview(
        method: String,
        receiver: String,
        amount: BigInteger,
        token: Token,
        estimatedFee: BigInteger,
        canShowDetails: Boolean,
        isEnoughFunds: Boolean,
        sponsor: String?,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        getShownBottomSheet().showTransactionRequestReview { (view, lifecycleOwner) ->
            initTransactionRequestReviewView(
                view = view,
                lifecycleOwner = lifecycleOwner,
                method = method,
                receiver = receiver,
                amount = amount,
                token = token,
                estimatedFee = estimatedFee,
                canShowDetails = canShowDetails,
                isEnoughFunds = isEnoughFunds,
                sponsor = sponsor,
                account = account,
                appMetadata = appMetadata,
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initTransactionRequestReviewView(
        view: FragmentWalletConnectTransactionRequestReviewBinding,
        lifecycleOwner: LifecycleOwner,
        method: String,
        receiver: String,
        amount: BigInteger,
        token: Token,
        estimatedFee: BigInteger,
        canShowDetails: Boolean,
        isEnoughFunds: Boolean,
        sponsor: String?,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) = with(view) {
        Glide.with(appIconImageView.context)
            .load(appMetadata.iconUrl)
            .placeholder(ThemedCircularProgressDrawable(root.context))
            .fitCenter()
            .into(appIconImageView)

        appNameTextView.text = appMetadata.name

        methodTextView.text = method
        receiverTextView.text = receiver

        with(selectedAccountInclude) {
            accAddress.text = account.getAccountName()
            accBalance.text = root.context.getString(
                R.string.acc_balance_placeholder,
                "${CurrencyUtil.formatGTU(token.balance, token)} ${token.symbol}"
            )
            accBalanceAtDisposal.isVisible = true
            accBalanceAtDisposal.text = root.context.getString(
                R.string.acc_balance_at_disposal_placeholder,
                CurrencyUtil.formatGTU(
                    account.balanceAtDisposal
                )
            )
            accIdentity.isVisible = true
            accIdentity.text = viewModel.getIdentityFromRepository(account)?.name ?: ""
        }

        amountTextView.text =
            "${CurrencyUtil.formatGTU(amount, token)} ${token.symbol}"

        if (sponsor != null) {
            feeTextView.isVisible = false
            sponsoredLabel.isVisible = true
        } else {
            feeTextView.isVisible = true
            feeTextView.text =
                root.context.getString(R.string.amount, CurrencyUtil.formatGTU(estimatedFee))
            sponsoredLabel.isVisible = false
        }

        errorTextView.isVisible = !isEnoughFunds
        errorTextView.text =
            when {
                !isEnoughFunds ->
                    root.context.getString(R.string.wallet_connect_transaction_request_insufficient_funds)

                else ->
                    null
            }

        with(showDetailsButton) {
            isVisible = canShowDetails
            if (canShowDetails) {
                setOnClickListener {
                    viewModel.onShowTransactionRequestDetailsClicked()
                }
            }
        }

        declineButton.setOnClickListener {
            viewModel.rejectSessionRequest()
        }

        approveButton.setOnClickListener {
            viewModel.approveSessionRequest()
        }
        viewModel.isSessionRequestApproveButtonEnabledFlow.collect(
            lifecycleOwner = lifecycleOwner,
            action = approveButton::setEnabled
        )
    }

    private fun showTransactionSubmitted(
        submissionId: String,
        estimatedFee: BigInteger,
    ) {
        getShownBottomSheet().showTransactionSubmitted { (view, _) ->
            initTransactionSubmittedView(
                view = view,
                submissionId = submissionId,
                estimatedFee = estimatedFee,
            )
        }
    }

    private fun initTransactionSubmittedView(
        view: FragmentWalletConnectTransactionSubmittedFragmentBinding,
        submissionId: String,
        estimatedFee: BigInteger,
    ) = with(view) {
        totalAmountTextView.isVisible = false
        totalAmountTitleTextView.isVisible = false
        totalAmountDivider.isVisible = false

        feeTextView.text =
            if (estimatedFee.signum() == 0)
                root.context.getString(R.string.wallet_connect_transaction_request_free_transaction)
            else
                root.context.getString(R.string.amount, CurrencyUtil.formatGTU(estimatedFee))

        transactionHashView.transactionHash = submissionId

        finishButton.setOnClickListener {
            viewModel.onTransactionSubmittedFinishClicked()
        }
    }


    private fun showSignRequestReview(
        message: String,
        canShowDetails: Boolean,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        getShownBottomSheet().showSignMessageReview { (view, lifecycleOwner) ->
            initSignRequestReviewView(
                view = view,
                lifecycleOwner = lifecycleOwner,
                message = message,
                canShowDetails = canShowDetails,
                account = account,
                appMetadata = appMetadata
            )
        }
    }

    private fun initSignRequestReviewView(
        view: FragmentWalletConnectSignMessageReviewBinding,
        lifecycleOwner: LifecycleOwner,
        message: String,
        canShowDetails: Boolean,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) = with(view) {
        Glide.with(appIconImageView.context)
            .load(appMetadata.iconUrl)
            .placeholder(ThemedCircularProgressDrawable(root.context))
            .fitCenter()
            .into(appIconImageView)

        appNameTextView.text = appMetadata.name

        with(selectedAccountInclude) {
            accAddress.text = account.getAccountName()
            accBalance.isVisible = false
            accIdentity.isVisible = true
            accIdentity.text = viewModel.getIdentityFromRepository(account)?.name ?: ""
        }

        messageTextView.text = message

        with(showDetailsButton) {
            isVisible = canShowDetails
            if (canShowDetails) {
                setOnClickListener {
                    viewModel.onShowSignRequestDetailsClicked()
                }
            }
        }

        declineButton.setOnClickListener {
            viewModel.rejectSessionRequest()
        }

        approveButton.setOnClickListener {
            viewModel.approveSessionRequest()
        }
        viewModel.isSessionRequestApproveButtonEnabledFlow.collect(
            lifecycleOwner = lifecycleOwner,
            action = approveButton::setEnabled
        )
    }

    private fun showIdentityProofRequestReview(
        claims: List<IdentityProofRequestClaims>,
        appMetadata: WalletConnectViewModel.AppMetadata,
        currentClaim: Int,
        provableState: WalletConnectViewModel.ProofProvableState,
    ) {
        getShownBottomSheet().showIdentityProofRequestReview { (view, lifecycleOwner) ->
            initIdentityProofRequestReview(
                view = view,
                lifecycleOwner = lifecycleOwner,
                appMetadata = appMetadata,
                claims = claims,
                currentClaim = currentClaim,
                provableState = provableState
            )
        }
    }

    private fun initIdentityProofRequestReview(
        view: FragmentWalletConnectIdentityProofRequestReviewBinding,
        lifecycleOwner: LifecycleOwner,
        appMetadata: WalletConnectViewModel.AppMetadata,
        claims: List<IdentityProofRequestClaims>,
        currentClaim: Int,
        provableState: WalletConnectViewModel.ProofProvableState,
    ) = with(view) {
        Glide.with(appIconImageView.context)
            .load(appMetadata.iconUrl)
            .placeholder(ThemedCircularProgressDrawable(root.context))
            .fitCenter()
            .into(appIconImageView)

        appNameTextView.text = appMetadata.name

        nextButton.setOnClickListener {
            this.proofView.currentItem++
        }

        approveButton.setOnClickListener {
            viewModel.approveSessionRequest()
        }

        declineButton.setOnClickListener {
            viewModel.rejectSessionRequest()
        }

        viewModel.isSessionRequestApproveButtonEnabledFlow.collect(
            lifecycleOwner = lifecycleOwner,
            action = approveButton::setEnabled
        )
        when (provableState) {
            WalletConnectViewModel.ProofProvableState.UnProvable -> {
                view.unprovableStatement.visibility = VISIBLE
            }
            // Specially handle if there are no compatible issuer
            WalletConnectViewModel.ProofProvableState.NoCompatibleIssuer -> {
                view.proofView.visibility = INVISIBLE
                view.pagerDots.visibility = GONE
                view.unprovableStatement.visibility = VISIBLE
                view.unprovableStatement.setText(R.string.noCredentialsForThatIssuer)
                view.nextButton.visibility = GONE
                return@with
            }

            else -> {
                view.unprovableStatement.visibility = GONE
            }
        }

        val adapter = CredentialStatementAdapter(
            claims = claims,
            onChangeAccountClicked = viewModel::onChangeIdentityProofAccountClicked,
            onIdentityChangeClicked = viewModel::onChangeIdentityProofIdentityClicked,
        )

        fun updatePrimaryActionButton() {
            val currentPosition = proofView.currentItem
            approveButton.isInvisible = currentPosition < claims.size - 1
            nextButton.isVisible = approveButton.isInvisible
        }
        this.proofView.adapter = adapter
        this.proofView.setCurrentItem(currentClaim, false)
        this.proofView.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePrimaryActionButton()
            }
        })

        updatePrimaryActionButton()

        // Connect tab dots to the proofView pager
        TabLayoutMediator(pagerDots, this.proofView) { _, _ -> }.attach()
    }

    private fun showConnecting() {
        getShownBottomSheet().showProgress { (view, _) ->
            initConnectingView(
                view = view,
            )
        }
    }

    private fun initConnectingView(
        view: FragmentWalletConnectProgressBinding,
    ) = with(view) {
        progressTextView.text = root.context.getString(R.string.wallet_connect_connecting)
    }

    private fun hideBottomSheet() {
        findBottomSheet()?.dismiss()
    }

    private fun findBottomSheet(): WalletConnectBottomSheet? =
        fragmentManager.findFragmentByTag(BOTTOM_SHEET_TAG) as? WalletConnectBottomSheet

    private fun getShownBottomSheet(): WalletConnectBottomSheet =
        findBottomSheet()
            ?: WalletConnectBottomSheet()
                .also { bottomSheet ->
                    fragmentManager.commitNow {
                        setReorderingAllowed(true)
                        add(bottomSheet, BOTTOM_SHEET_TAG)
                    }
                    (bottomSheet.dialog as? BottomSheetDialog)?.also(::initBottomSheetDialog)
                }

    private fun initBottomSheetDialog(dialog: BottomSheetDialog) = with(dialog) {
        setOnCancelListener {
            viewModel.onDialogCancelled()
        }

        // Make the view model handle back pressed.
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.onBackPressed()) {
                    cancel()
                }
            }
        })
    }

    private companion object {
        private const val BOTTOM_SHEET_TAG = "wc_bottom_sheet"
    }
}
