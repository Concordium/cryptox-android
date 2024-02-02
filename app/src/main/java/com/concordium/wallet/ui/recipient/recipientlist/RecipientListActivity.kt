package com.concordium.wallet.ui.recipient.recipientlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ActivityRecipientListBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.recipient.RecipientActivity
import com.concordium.wallet.util.Log

class RecipientListActivity :
    BaseActivity(R.layout.activity_recipient_list, R.string.recipient_list_default_title), INotification {

    companion object {
        const val EXTRA_SELECT_RECIPIENT_MODE = "EXTRA_SELECT_RECIPIENT_MODE"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
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

        val selectRecipientMode = intent.getBooleanExtra(EXTRA_SELECT_RECIPIENT_MODE, false)
        val isShielded = intent.getBooleanExtra(EXTRA_SHIELDED, false)
        val account = intent.getSerializableExtra(EXTRA_ACCOUNT) as? Account

        initializeViewModel()
        viewModel.initialize(selectRecipientMode, isShielded, account)
        initializeViews()

        hideAddContact(isVisible = true) {
            val intent = Intent(this, RecipientActivity::class.java)
            intent.putExtra(RecipientActivity.EXTRA_GOTO_SCAN_QR, false)
            startActivity(intent)
        }
        hideQrScan(isVisible = true) {
            gotoScanBarCode()
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
        if (viewModel.selectRecipientMode) {
            setActionBarTitle(R.string.recipient_list_select_title)
        } else {
            // Hide shield/unshield function in address book
            binding.recipientShieldContainer.visibility = View.GONE
        }
//        scan_qr_imageview.setOnClickListener {
//            gotoScanBarCode()
//        }

        binding.recipientSearchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(txt: String?): Boolean {
                recipientAdapter.filter(txt)
                return true
            }

            override fun onQueryTextChange(txt: String?): Boolean {
                recipientAdapter.filter(txt)
                return true
            }
        })
        initializeList()

        /*
        viewModel.account?.let {
            val shieldHeaderTextRes = if (viewModel.isShielded) R.string.recipient_list_unshield_amount else R.string.recipient_list_shield_amount
            recipient_own_account.findViewById<TextView>(R.id.recipient_name_textview).setText(shieldHeaderTextRes)
            recipient_own_account.findViewById<TextView>(R.id.recipient_address_textview).setText(it.address)
        }
        recipient_own_account.setOnClickListener(View.OnClickListener {
            viewModel.account?.let {
                goBackToSendFunds(Recipient(it.id, it.name, it.address))
            }
        })*/
    }

    private fun initializeList() {
        recipientAdapter = RecipientAdapter(object : IListCallback {
            override fun delete(item: Recipient) {
                Log.d("Delete")
                confirmationBottomSheet?.setData(description = "do you really want to delete ${item.name} contact?", data = item)
            }

            override fun handleRowClick(item: Recipient) {
                if (viewModel.selectRecipientMode) {
                    goBackWithRecipient(item)
                } else {
                    gotoEditRecipient(item)
                }
            }
        })
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.adapter = recipientAdapter
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(RecipientActivity.EXTRA_GOTO_SCAN_QR, true)
        startActivity(intent)
    }

    private fun gotoNewRecipient() {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(
            RecipientActivity.EXTRA_SELECT_RECIPIENT_MODE,
            viewModel.selectRecipientMode
        )
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
