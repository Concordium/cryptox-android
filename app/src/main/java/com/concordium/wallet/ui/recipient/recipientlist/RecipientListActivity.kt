package com.concordium.wallet.ui.recipient.recipientlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ActivityRecipientListBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.recipient.RecipientActivity
import com.concordium.wallet.ui.scanqr.ScanQRActivity
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.getOptionalSerializable

class RecipientListActivity :
    BaseActivity(R.layout.activity_recipient_list, R.string.recipient_list_default_title),
    INotification {

    companion object {
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_SENDER_ACCOUNT = "EXTRA_SENDER_ACCOUNT"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
    }

    private var confirmationBottomSheet: ConfirmationBottomSheet? = null
    private lateinit var viewModel: RecipientListViewModel
    private val binding by lazy {
        ActivityRecipientListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var recipientAdapter: RecipientAdapter

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize(
            isShielded = intent.getBooleanExtra(EXTRA_SHIELDED, false),
            senderAccount = intent.getOptionalSerializable(
                EXTRA_SENDER_ACCOUNT,
                Account::class.java
            )
        )
        initializeViews()

        hideAddContact(isVisible = true) {
            val intent = Intent(this, RecipientActivity::class.java)
            intent.putExtra(RecipientActivity.EXTRA_GOTO_SCAN_QR, false)
            startActivity(intent)
        }

        hideActionBarBack(isVisible = true)
        hideLeftPlus(isVisible = false) {
            gotoNewRecipient()
        }

        confirmationBottomSheet = ConfirmationBottomSheet(this, this)
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecipientListViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }

        viewModel.recipientListLiveData.observe(this) {
            it.let {
                recipientAdapter.setData(it)
                showWaiting(false)
            }
        }
    }

    private fun initializeViews() {
        showWaiting(true)
        binding.scanQrIcon.setOnClickListener {
            gotoScanBarCode()
        }

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

        initializeList()
    }

    private val gerResultQRScan =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
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
                    goBackWithRecipient(
                        Recipient(
                            address = text,
                        )
                    )
                }
            } else {
                binding.continueBtn.isVisible = false
            }
        }
        showSearchIcons(text)
        recipientAdapter.filter(text)
    }

    private fun initializeList() {
        recipientAdapter = RecipientAdapter(object : IListCallback {
            override fun delete(item: Recipient) {
                Log.d("Delete")
                confirmationBottomSheet?.setData(
                    description = "Do you really want to delete ${item.name} contact?",
                    data = item
                )
            }

            override fun handleRowClick(item: Recipient) {
                if (viewModel.canGoBackWithRecipientAddress(item.address)) {
                    goBackWithRecipient(item)
                } else {
                    gotoEditRecipient(item)
                }
            }
        })
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.adapter = recipientAdapter
    }

    private fun showSearchIcons(text: String?) {
        val visible = text.isNullOrEmpty()
        binding.apply {
            searchLabel.isVisible = visible
            searchIcon.isVisible = visible
            scanQrIcon.isVisible = visible
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, ScanQRActivity::class.java)
        gerResultQRScan.launch(intent)
    }

    private fun gotoNewRecipient() {
        val intent = Intent(this, RecipientActivity::class.java)
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
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun confirmDeleteContact(data: Any?) {
        (data as? Recipient)?.let {
            viewModel.deleteRecipient(it)
        }
    }

    override fun cancelDeleteContact() {
    }
}
