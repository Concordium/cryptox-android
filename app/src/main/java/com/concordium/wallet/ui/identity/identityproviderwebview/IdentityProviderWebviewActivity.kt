package com.concordium.wallet.ui.identity.identityproviderwebview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.model.IdentityCreationData
import com.concordium.wallet.data.preferences.WalletIdentityCreationDataPreferences
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentityProviderWebviewBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import java.util.BitSet

class IdentityProviderWebviewActivity : BaseActivity(
    R.layout.activity_identity_provider_webview,
    R.string.identity_provider_webview_title
) {

    private lateinit var viewModel: IdentityProviderWebViewViewModel
    private val binding by lazy {
        ActivityIdentityProviderWebviewBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private var showForFirstIdentity = false
    private var chromeLaunched = false

    companion object {
        const val EXTRA_IDENTITY_CREATION_DATA = "EXTRA_IDENTITY_CREATION_DATA"
        const val EXTRA_SHOW_FOR_FIRST_IDENTITY = "SHOW_FOR_FIRST_IDENTITY"
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_DATAWEREAVAILABLE = 1 // data were available
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_VALIDCALLBACKURI = 2 // valid callback uri
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOIDENTITYDATA =
            3 // identityCreationData was not null
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_MODELSINITIALISED =
            4 // models initialised correctly
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_INCORRECTCALLBACKURI =
            5 // incorrect callback URL
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_CODEURISET =
            6 // code_uri is set in callback uri
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_TOKENSET = 7 // token is set in callback uri
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_ERRORSET =
            8 // error is set (given other errors)
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOTHINGSET =
            9 // nothing is set in the callback uri
    }

    private val preferences: WalletIdentityCreationDataPreferences=
        App.appCore.session.walletStorage.identityCreationDataPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showForFirstIdentity = preferences.getShowForFirstIdentityFromCallback()
        preferences.setShowForFirstIdentityFromCallback(false)

        if (!showForFirstIdentity)
            showForFirstIdentity =
                intent.extras?.getBoolean(EXTRA_SHOW_FOR_FIRST_IDENTITY, false) ?: false

        var handled = false

        val supportCode = BitSet(10) // room for 10 flags
        val identityCreationData = preferences.getIdentityCreationData()

        // In the case where the activity has been force closed, onCreate needs to handle to receive the callback uri
        intent.data?.let { uri ->
            supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_DATAWEREAVAILABLE) // data were available
            if (hasValidCallbackUri(uri)) {
                supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_VALIDCALLBACKURI) // valid callback
                if (identityCreationData != null) {
                    supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOIDENTITYDATA) // identityCreationData was not null
                    handled = true
                    initializeViewModel()
                    viewModel.initialize(identityCreationData)
                    // Clear the temp data, now that we have set in on the view model
                    preferences.setIdentityCreationData(null)
                    initViews()
                    supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_MODELSINITIALISED) // models initialised correctly
                    handleNewIntentData(uri, supportCode)
                }
            }
        }
        if (!handled) {
            // Initial case, where we come from the previous page
            val tempData =
                intent.extras!!.getSerializable(EXTRA_IDENTITY_CREATION_DATA) as IdentityCreationData?
            if (tempData != null) {
                preferences.setIdentityCreationData(tempData)
                handled = true
                initializeViewModel()
                viewModel.initialize(tempData)
                initViews()
                if (!viewModel.useTemporaryBackend) {
                    viewModel.beginVerification()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (chromeLaunched)
            finish()

        runCatching {
            App.appCore.tracker.identityVerificationScreen(
                provider = viewModel.identityCreationData.identityProvider.displayName
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (chromeLaunched)
            finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            handleNewIntentData(it, null)
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityProviderWebViewViewModel::class.java]
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.handleIdentityVerificationUri.observe(this, object : EventObserver<Uri>() {
            override fun onUnhandledEvent(value: Uri) {
                if (hasValidCallbackUri(value)) {
                    handleNewIntentData(value, null)
                } else {
                    showChromeCustomTab(value.toString())
                }
            }
        })
        viewModel.identityCreationError.observe(this, object : EventObserver<String>() {
            override fun onUnhandledEvent(value: String) {
                val error = BackendError(0, value)
                gotoFailed(error)
            }
        })
        viewModel.identityCreationUserCancel.observe(this, object : EventObserver<String>() {
            override fun onUnhandledEvent(value: String) {
                onBackPressed()
            }
        })
        viewModel.gotoIdentityConfirmedLiveData.observe(this, object : EventObserver<Identity>() {
            override fun onUnhandledEvent(value: Identity) {
                gotoIdentityConfirmed(value)
            }
        })
        viewModel.gotoFailedLiveData.observe(
            this,
            object : EventObserver<Pair<Boolean, BackendError?>>() {
                override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                    if (value.first) {
                        gotoFailed(value.second)
                    }
                }
            })
    }

    fun initViews() {
        val title =
            viewModel.identityCreationData.identityProvider.displayName + " " +
                    getString(R.string.identity_provider_webview_title)
        setActionBarTitle(title)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoIdentityConfirmed(identity: Identity) {
        finish()
        if (showForFirstIdentity) {
            finishAffinity()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            val intent = Intent(this, IdentityConfirmedActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(IdentityConfirmedActivity.EXTRA_IDENTITY, identity)
            startActivity(intent)
        }
    }

    private fun gotoFailed(error: BackendError?) {
        finish()
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Identity)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun hasValidCallbackUri(uri: Uri): Boolean {
        if (uri.toString().startsWith(IdentityProviderWebViewViewModel.CALLBACK_URL)) {
            return true
        }
        return false
    }

    private fun handleNewIntentData(uri: Uri, supportCode: BitSet?): Boolean {
        if (uri.toString().startsWith(IdentityProviderWebViewViewModel.CALLBACK_URL)) {
            val fragment = uri.fragment
            val fragmentParts = fragment?.split("=")
            if (!fragmentParts.isNullOrEmpty() && fragmentParts[0] == "code_uri") {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_CODEURISET) // code_uri is set
                viewModel.parseIdentityAndSavePending(uri.toString().split("#code_uri=").last())
            } else if (!fragmentParts.isNullOrEmpty() && fragmentParts[0] == "token") {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_TOKENSET) // token is set
                val identity = fragmentParts[1]
                viewModel.parseIdentityAndSave(identity)
            } else if (!fragmentParts.isNullOrEmpty() && fragmentParts[0] == "error") {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_ERRORSET) // error is set
                viewModel.parseIdentityError(fragmentParts[1])
            } else {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOTHINGSET) // nothing is set
            }
            return true
        }
        supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_INCORRECTCALLBACKURI) // incorrect callback URL
        return false
    }

    private fun showChromeCustomTab(url: String) {
        // Try to use Chrome browser to show url, if Chrome is not installed an ActivityNotFoundException will be thrown
        try {
            launchChromeCustomTab(url, true)
            preferences.setShowForFirstIdentityFromCallback(showForFirstIdentity)
            chromeLaunched = true
            return
        } catch (e: ActivityNotFoundException) {
        }
        // If not Chrome let the default browser with Custom Tabs handle this
        try {
            launchChromeCustomTab(url)
            preferences.setShowForFirstIdentityFromCallback(showForFirstIdentity)
            chromeLaunched = true
            return
        } catch (e: ActivityNotFoundException) {
            showError(R.string.app_error_general)
        }
    }

    private fun launchChromeCustomTab(url: String, forceChromeBrowser: Boolean = false) {
        val customTabBuilder = CustomTabsIntent.Builder()
        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(this, R.color.theme_white))
            .build()
        customTabBuilder.setDefaultColorSchemeParams(colorSchemeParams)
        val customTabsIntent = customTabBuilder.build()
        if (forceChromeBrowser) {
            customTabsIntent.intent.setPackage("com.android.chrome")
        }

        customTabsIntent.launchUrl(this, Uri.parse(url))
    }
}
