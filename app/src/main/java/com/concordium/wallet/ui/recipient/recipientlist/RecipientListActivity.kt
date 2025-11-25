package com.concordium.wallet.ui.recipient.recipientlist

import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ActivityRecipientListBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.recipient.RecipientActivity
import com.concordium.wallet.ui.scanqr.ScanQRActivity
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import com.concordium.wallet.util.getOptionalSerializable

class RecipientListActivity : BaseActivity(R.layout.activity_recipient_list) {

    companion object {
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_SENDER_ACCOUNT = "EXTRA_SENDER_ACCOUNT"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
    }

    private lateinit var viewModel: RecipientListViewModel
    private val binding by lazy {
        ActivityRecipientListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var recipientAdapter: RecipientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        val senderAccount = intent.getOptionalSerializable(
            EXTRA_SENDER_ACCOUNT,
            Account::class.java
        )
        viewModel.initialize(
            isShielded = intent.getBooleanExtra(EXTRA_SHIELDED, false),
            senderAccount = senderAccount
        )
        if (senderAccount != null) {
            setActionBarTitle(R.string.recipient_list_select_title)
            hideAddContact(isVisible = false)
            hideQrScan(isVisible = true) {
                gotoScanBarCode()
            }
        } else {
            setActionBarTitle(R.string.recipient_list_default_title)
            hideAddContact(isVisible = true) {
                gotoNewRecipient()
            }
            hideQrScan(isVisible = false)
        }
        initializeViews(senderAccount != null)
        hideActionBarBack(isVisible = true)
        initObservers()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecipientListViewModel::class.java]

        viewModel.waiting.collectWhenStarted(this) {
            showWaiting(it)
        }

        viewModel.filteredList.collectWhenStarted(this) { list ->
            recipientAdapter.setData(list)
            showWaiting(false)
        }
    }

    private fun initializeViews(isSelectMode: Boolean) {
        showWaiting(true)

        binding.recipientSearchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(txt: String?): Boolean {
                onSearchTextChanged(txt)
                return true
            }

            override fun onQueryTextChange(txt: String?): Boolean {
                onSearchTextChanged(txt)
                return true
            }
        })

        listOf(binding.searchLayout, binding.searchIcon).forEach {
            it.setOnClickListener {
                showKeyboard(this, binding.recipientSearchview)
            }
        }

        initializeList(isSelectMode)
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            DeleteRecipientBottomSheet.ACTION_DELETE,
            this
        ) { _, bundle ->
            if (DeleteRecipientBottomSheet.getResult(bundle)) {
                viewModel.deleteRecipient()
            } else {
                viewModel.setRecipient(null)
            }
        }
    }

    private val gerResultQRScan =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.getStringExtra(Constants.Extras.EXTRA_SCANNED_QR_CONTENT)
                    ?.let { address ->
                        binding.recipientSearchview.setQuery(address, true)
                    }
            }
        }

    private fun onSearchTextChanged(text: String?) {
        if (text != null) {
            if (viewModel.canGoBackWithRecipientAddress(text)) {
                binding.continueBtn.isVisible = true
                binding.continueBtn.setOnClickListener {
                    goBackWithRecipient(Recipient(address = text))
                }
            } else {
                binding.continueBtn.isVisible = false
            }
        }
        viewModel.filter(text)
        showSearchViews(text)
    }

    private fun initializeList(isSelectMode: Boolean) {
        recipientAdapter = RecipientAdapter(
            callback = object : IListCallback {
                override fun delete(item: RecipientListItem.RecipientItem) {
                    viewModel.setRecipient(item.toRecipient())
                    DeleteRecipientBottomSheet.newInstance(
                        DeleteRecipientBottomSheet.setBundle(
                            item.name
                        )
                    ).showSingle(supportFragmentManager, DeleteRecipientBottomSheet.TAG)
                }

                override fun handleRowClick(item: RecipientListItem.RecipientItem) {
                    if (viewModel.canGoBackWithRecipientAddress(item.address)) {
                        goBackWithRecipient(Recipient(item.address))
                    } else {
                        gotoEditRecipient(item.toRecipient())
                    }
                }
            },
            isSelectMode = isSelectMode
        )
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.adapter = recipientAdapter
    }

    private fun showSearchViews(text: String?) {
        val visible = text.isNullOrEmpty()
        binding.apply {
            searchLabel.isVisible = visible
            searchIcon.isVisible = visible
            searchResultsLabel.isVisible = !visible && viewModel.filteredList.value.isNotEmpty()
            searchNoResults.isVisible =
                !visible && viewModel.filteredList.value.isEmpty() &&
                        !viewModel.canGoBackWithRecipientAddress(text ?: "")
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.progress.progressLayout.isVisible = waiting
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        gerResultQRScan.launch(intent)
    }

    private fun gotoNewRecipient() {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(RecipientActivity.EXTRA_GOTO_SCAN_QR, false)
        startActivity(intent)
    }

    private fun gotoEditRecipient(recipient: Recipient) {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(RecipientActivity.EXTRA_RECIPIENT, recipient)
        startActivity(intent)
    }

    private fun goBackWithRecipient(recipient: Recipient) {
        val intent = Intent()
        intent.putExtra(EXTRA_RECIPIENT, recipient)
        setResult(RESULT_OK, intent)
        finish()
    }
}
