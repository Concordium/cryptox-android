package com.concordium.wallet.ui.account.newaccountname

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityNewAccountNameBinding
import com.concordium.wallet.ui.account.newaccountidentity.NewAccountIdentityActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.ValidationUtil

class NewAccountNameActivity :
    BaseActivity(R.layout.activity_new_account_name, R.string.new_account_name_title) {

    private lateinit var viewModel: NewAccountNameViewModel
    private val binding by lazy {
        ActivityNewAccountNameBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initViews()
        hideActionBarBack(isVisible = true)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountNameViewModel::class.java]
    }

    private fun initViews() {
        binding.nextButton.setOnClickListener {
            gotoIdentityList()
        }

        binding.accountNameEdittext.afterTextChanged { text ->
            binding.nextButton.isEnabled = text.isNotEmpty()
        }

        binding.accountNameEdittext.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (textView.text.isNotEmpty())
                        gotoIdentityList()
                    true
                }
                else -> false
            }
        }
        binding.accountNameEdittext.requestFocus()
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun gotoIdentityList() {
        if (!ValidationUtil.validateName(binding.accountNameEdittext.text.toString())) {
            binding.accountNameEdittext.error = getString(R.string.valid_special_chars_error_text)
            return
        }

        val intent = Intent(this, NewAccountIdentityActivity::class.java)
        intent.putExtra(
            NewAccountIdentityActivity.EXTRA_ACCOUNT_NAME,
            binding.accountNameEdittext.text.toString()
        )
        startActivity(intent)
    }

    //endregion
}
