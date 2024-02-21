package com.concordium.wallet.ui.walletconnect

import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.reflect.full.createInstance

class WalletConnectBottomSheet : BottomSheetDialogFragment(
    R.layout.fragment_wallet_connect_bottom_sheet
) {
    override fun getTheme() =
        R.style.AppBottomSheetDialogTheme

    fun showSessionProposalReview(
        onShown: (createdView: WalletConnectSessionProposalReviewFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectSessionProposalReviewFragment =
            getShownFragment(SESSION_PROPOSAL_REVIEW_TAG)

        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    fun showAccountSelection(
        onShown: (createdView: WalletConnectAccountSelectionFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectAccountSelectionFragment =
            getShownFragment(ACCOUNT_SELECTION_TAG)

        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    fun showTransactionRequestReview(
        onShown: (createdView: WalletConnectTransactionRequestReviewFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectTransactionRequestReviewFragment =
            getShownFragment(TRANSACTION_REQUEST_REVIEW_TAG)

        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    fun showTransactionSubmitted(
        onShown: (createdView: WalletConnectTransactionSubmittedFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectTransactionSubmittedFragment =
            getShownFragment(TRANSACTION_SUBMITTED_TAG)

        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    fun showSignRequestReview(
        onShown: (createdView: WalletConnectSignRequestReviewFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectSignRequestReviewFragment =
            getShownFragment(SIGN_REQUEST_REVIEW_TAG)

        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    fun showIdentityProofRequestReview(
        onShown: (createdView: WalletConnectIdentityProofRequestReviewFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectIdentityProofRequestReviewFragment =
            getShownFragment(IDENTITY_PROOF_REQUEST_REVIEW_TAG)
1
        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    fun showProgress(
        onShown: (createdView: WalletConnectProgressFragment.CreatedView) -> Unit
    ) {
        val fragment: WalletConnectProgressFragment =
            getShownFragment(PROGRESS_TAG)

        fragment.createdView.observe(this) { createdView ->
            onShown(createdView)
            fragment.createdView.removeObservers(this)
        }
    }

    private inline fun <reified T : Fragment> getShownFragment(tag: String): T =
        childFragmentManager.findFragmentByTag(tag) as? T
            ?: T::class.createInstance().also { newInstance ->
                childFragmentManager.commit {
                    disallowAddToBackStack()
                    replace(R.id.fragment_container, newInstance, tag)
                }
            }

    private companion object {
        private const val SESSION_PROPOSAL_REVIEW_TAG = "wc_spr"
        private const val ACCOUNT_SELECTION_TAG = "wc_acc"
        private const val TRANSACTION_REQUEST_REVIEW_TAG = "wc_trr"
        private const val SIGN_REQUEST_REVIEW_TAG = "wc_srr"
        private const val IDENTITY_PROOF_REQUEST_REVIEW_TAG = "wc_id"
        private const val TRANSACTION_SUBMITTED_TAG = "wc_ts"
        private const val PROGRESS_TAG = "wc_progress"
    }
}
