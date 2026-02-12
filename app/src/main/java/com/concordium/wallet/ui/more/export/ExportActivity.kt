package com.concordium.wallet.ui.more.export

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.concordium.wallet.DataFileProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityExportBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.KeyboardUtil
import java.io.File

class ExportActivity : BaseActivity(
    R.layout.activity_export,
    R.string.export_title
), AuthDelegate by AuthDelegateImpl() {

    private var isShareFlowActive: Boolean = false

    private companion object {
        private const val BACKSTACK_NAME_PASSWORD = "BACKSTACK_NAME_PASSWORD"
        private const val BACKSTACK_NAME_REPEAT_PASSWORD = "BACKSTACK_NAME_REPEAT_PASSWORD"
    }

    private val viewModel: ExportViewModel by viewModels()
    private val binding by lazy {
        ActivityExportBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        viewModel.initialize()
        initViews()
        if (null == savedInstanceState) {
            replaceFragment(ExportFragment(R.string.export_title), "", false)
        }
    }

    override fun onResume() {
        super.onResume()
        isShareFlowActive = false
    }

    override fun onStop() {
        super.onStop() // called when app shared with is started
        if (isShareFlowActive) {
            finish()
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
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@ExportActivity,
                        onAuthenticated = viewModel::checkLogin
                    )
                }
            }
        })
        viewModel.showRepeatPasswordScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showRepeatExportPassword()
                }
            }
        })
        viewModel.showRequestPasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showRequestExportPassword()
                }
            }
        })
        viewModel.shareExportFileLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    shareExportFile()
                }
            }
        })

        viewModel.finishRepeatPasswordScreenLiveData.observe(
            this,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    supportFragmentManager.popBackStack(
                        BACKSTACK_NAME_REPEAT_PASSWORD,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    if (value) {
                        // Success
                        supportFragmentManager.popBackStack(
                            BACKSTACK_NAME_PASSWORD,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        viewModel.finalizeEncryptionOfFile()
                    } else {
                        // Failure
                    }
                }
            })
    }

    private fun initViews() {
        binding.progress.progressLayout.visibility = View.GONE
    }

    //endregion

    //region Control
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        binding.progress.progressLayout.isVisible = waiting
    }

    private fun showErrorMessage(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        Toast.makeText(applicationContext, stringRes, Toast.LENGTH_SHORT).show()
    }

    private fun showRequestExportPassword() {
        replaceFragment(
            ExportSetupPasswordFragment(R.string.export_setup_password_title),
            BACKSTACK_NAME_PASSWORD,
            true
        )
    }

    private fun showRepeatExportPassword() {
        replaceFragment(
            ExportSetupPasswordRepeatFragment(R.string.export_setup_password_repeat_title),
            BACKSTACK_NAME_REPEAT_PASSWORD,
            true
        )
    }

    private fun shareExportFile() {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "message/rfc822"
        val uri =
            Uri.parse("content://" + DataFileProvider.AUTHORITY.toString() + File.separator.toString() + ExportViewModel.FILE_NAME)
        share.putExtra(Intent.EXTRA_STREAM, uri)
        val resInfoList =
            this.packageManager.queryIntentActivities(share, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        startActivity(Intent.createChooser(share, null))
        isShareFlowActive = true
    }

    private fun replaceFragment(fragment: Fragment, name: String, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(name)
        }
        transaction.commit()
    }
    //endregion
}
