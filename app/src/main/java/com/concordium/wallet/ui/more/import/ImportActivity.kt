package com.concordium.wallet.ui.more.import

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityImportBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.KeyboardUtil

class ImportActivity : BaseActivity(
    R.layout.activity_import,
    R.string.import_title
), AuthDelegate by AuthDelegateImpl() {

    companion object {
        const val EXTRA_FILE_URI = "EXTRA_FILE_URI"
        const val EXTRA_GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS = "GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS"
        const val REQUEST_CODE_SELECT_FILE = 2000
    }

    private val viewModel: ImportViewModel by viewModels()
    private val binding by lazy {
        ActivityImportBinding.bind(findViewById(R.id.root_layout))
    }
    private var allowBack = true
        set(value) {
            field = value
            hideActionBarBack(isVisible = value)
        }

    //region Lifecycle
    // ************************************************************

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize()
        initViews()
        if (null != savedInstanceState) {
            // Restoring state (screen orientation changed) - Do not add fragment or show dialog
            // again, because we will end up with multiples of the same fragment.
            return
        }

        val fileUri = intent?.getParcelableExtra<Uri>(EXTRA_FILE_URI)
        if (fileUri != null) {
            handleImportFile(fileUri)
        } else {
            showFilePicker()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.also { uri ->
                    handleImportFile(uri)
                }
            } else {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (allowBack) {
            super.onBackPressed()
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showErrorMessage(value)
            }
        })
        viewModel.showImportPasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showImportPassword()
                }
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@ImportActivity,
                        onAuthenticated = viewModel::checkLogin
                    )
                }
            }
        })
        viewModel.showImportConfirmedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showImportConfirmed()
                }
            }
        })
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value
                    && intent.getBooleanExtra(EXTRA_GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS, false)
                ) {
                    finishAffinity()
                    val intent = Intent(this@ImportActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                } else {
                    finish()
                }
            }
        })

        viewModel.errorAndFinishLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showFailedImport(value)
            }
        })
    }

    private fun initViews() {
        binding.progress.messageTextView.isVisible = true
        binding.progress.messageTextView.text = getString(R.string.import_importing_progress)
        binding.progress.progressLayout.visibility = View.VISIBLE
    }

    //endregion

    //region Control
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun showErrorMessage(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.root, stringRes)
    }

    private fun showImportPassword() {
        replaceFragment(ImportPasswordFragment(R.string.import_password_title))
        allowBack = true
    }

    private fun showImportConfirmed() {
        replaceFragment(ImportConfirmedFragment(R.string.import_confirmed_title))
        allowBack = false
    }

    private fun showFailedImport(message: Int) {
        replaceFragment(ImportFailedFragment(message, R.string.import_confirmed_title))
        allowBack = false
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    @Suppress("DEPRECATION")
    private fun showFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
    }

    private fun handleImportFile(uri: Uri) {
        viewModel.handleImportFile(ImportFile(uri))
    }

    //endregion
}
