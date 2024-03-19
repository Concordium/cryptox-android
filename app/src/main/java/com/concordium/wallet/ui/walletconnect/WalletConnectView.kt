package com.concordium.wallet.ui.walletconnect

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentWalletConnectAccountSelectionBinding
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofRequestReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectProgressBinding
import com.concordium.wallet.databinding.FragmentWalletConnectSessionProposalReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectSignRequestReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionRequestReviewBinding
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionSubmittedFragmentBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
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
                is WalletConnectViewModel.State.SessionRequestReview -> {
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

            WalletConnectViewModel.State.WaitingForSessionRequest -> {
                showConnecting()
            }

            is WalletConnectViewModel.State.SessionRequestReview.TransactionRequestReview -> {
                showTransactionRequestReview(
                    method = state.method,
                    receiver = state.receiver,
                    amount = state.amount,
                    estimatedFee = state.estimatedFee,
                    isEnoughFunds = state.isEnoughFunds,
                    account = state.account,
                    appMetadata = state.appMetadata,
                )
            }

            is WalletConnectViewModel.State.SessionRequestReview.SignRequestReview -> {
                showSignRequestReview(
                    message = state.message,
                    account = state.account,
                    appMetadata = state.appMetadata,
                )
            }

            is WalletConnectViewModel.State.SessionRequestReview.IdentityProofRequestReview -> {
                showIdentityProofRequestReview(
                    accounts = state.chosenAccounts,
                    appMetadata = state.appMetadata,
                    statements = state.request.credentialStatements,
                    currentStatement = state.currentStatement
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

                    WalletConnectViewModel.Error.LoadingFailed ->
                        R.string.wallet_connect_error_loading_failed

                    WalletConnectViewModel.Error.AccountMismatch ->
                        R.string.wallet_connect_error_account_mismatch

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

                Toast.makeText(activity, errorRes, Toast.LENGTH_SHORT).show()
            }
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
            accBalance.text = root.context.getString(
                R.string.acc_balance_placeholder,
                CurrencyUtil.formatGTU(
                    selectedAccount.getAtDisposalWithoutStakedOrScheduled(
                        selectedAccount.totalUnshieldedBalance
                    ), true
                )
            )
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

    private fun showTransactionRequestReview(
        method: String,
        receiver: String,
        amount: BigInteger,
        estimatedFee: BigInteger,
        isEnoughFunds: Boolean,
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
                estimatedFee = estimatedFee,
                isEnoughFunds = isEnoughFunds,
                account = account,
                appMetadata = appMetadata,
            )
        }
    }

    private fun initTransactionRequestReviewView(
        view: FragmentWalletConnectTransactionRequestReviewBinding,
        lifecycleOwner: LifecycleOwner,
        method: String,
        receiver: String,
        amount: BigInteger,
        estimatedFee: BigInteger,
        isEnoughFunds: Boolean,
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
                CurrencyUtil.formatGTU(
                    account.getAtDisposalWithoutStakedOrScheduled(
                        account.totalUnshieldedBalance
                    ), true
                )
            )
        }

        amountTextView.text =
            root.context.getString(R.string.amount, CurrencyUtil.formatGTU(amount))
        feeTextView.text =
            root.context.getString(R.string.amount, CurrencyUtil.formatGTU(estimatedFee))

        errorTextView.isVisible = !isEnoughFunds
        errorTextView.text =
            when {
                !isEnoughFunds ->
                    root.context.getString(R.string.wallet_connect_transaction_request_insufficient_funds)

                else ->
                    null
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
            root.context.getString(R.string.amount, CurrencyUtil.formatGTU(estimatedFee))

        transactionHashView.transactionHash = submissionId

        finishButton.setOnClickListener {
            viewModel.onTransactionSubmittedFinishClicked()
        }
    }


    private fun showSignRequestReview(
        message: String,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        getShownBottomSheet().showSignRequestReview { (view, lifecycleOwner) ->
            initSignRequestReviewView(
                view = view,
                lifecycleOwner = lifecycleOwner,
                message = message,
                account = account,
                appMetadata = appMetadata
            )
        }
    }

    private fun initSignRequestReviewView(
        view: FragmentWalletConnectSignRequestReviewBinding,
        lifecycleOwner: LifecycleOwner,
        message: String,
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
            accBalance.text = root.context.getString(
                R.string.acc_balance_placeholder,
                CurrencyUtil.formatGTU(
                    account.getAtDisposalWithoutStakedOrScheduled(
                        account.totalUnshieldedBalance
                    ), true
                )
            )
        }

        messageTextView.text = message

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
        statements: List<RequestStatement>,
        accounts: List<Account>,
        appMetadata: WalletConnectViewModel.AppMetadata,
        currentStatement: Int
    ) {
        getShownBottomSheet().showIdentityProofRequestReview { (view, lifecycleOwner) ->
            initIdentityProofRequestReview(
                view = view,
                lifecycleOwner = lifecycleOwner,
                accounts = accounts,
                appMetadata = appMetadata,
                statements = statements,
                currentStatement = currentStatement
            )
        }
    }

    private fun initIdentityProofRequestReview(
        view: FragmentWalletConnectIdentityProofRequestReviewBinding,
        lifecycleOwner: LifecycleOwner,
        accounts: List<Account>,
        appMetadata: WalletConnectViewModel.AppMetadata,
        statements: List<RequestStatement>,
        currentStatement: Int
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
        when (viewModel.getProofProvableState()) {
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
            statements, accounts, viewModel::getIdentity
        ) {
            viewModel.onChooseAccountIdentityProof(it)
        }
        this.proofView.adapter = adapter
        this.proofView.setCurrentItem(currentStatement, false)
        this.proofView.registerOnPageChangeCallback(object: OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == statements.size - 1) {
                    this@with.approveButton.visibility = VISIBLE
                    this@with.nextButton.visibility = GONE
                }
            }
        })

        // Connect tab dots to the proofView pager
        TabLayoutMediator(pagerDots,this.proofView) { _, _ -> }.attach()
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
