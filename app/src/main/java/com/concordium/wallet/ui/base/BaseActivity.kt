package com.concordium.wallet.ui.base

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.concordium.wallet.App
import com.concordium.wallet.Constants
import com.concordium.wallet.Constants.Extras.EXTRA_ADD_CONTACT
import com.concordium.wallet.Constants.Extras.EXTRA_AIR_DROP_PAYLOAD
import com.concordium.wallet.Constants.Extras.EXTRA_CONNECT_URL
import com.concordium.wallet.Constants.Extras.EXTRA_QR_CONNECT
import com.concordium.wallet.R
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.airdrop.AirdropActivity
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.connect.ConnectActivity
import com.concordium.wallet.ui.scanqr.ScanQRActivity
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import com.concordium.wallet.uicore.dialog.UnlockFeatureDialog
import com.concordium.wallet.uicore.popup.Popup
import java.io.Serializable
import javax.crypto.Cipher

abstract class BaseActivity(
    private val layout: Int? = null,
    private val titleId: Int = R.string.app_name
) : AppCompatActivity() {

    private var titleView: TextView? = null
    protected lateinit var popup: Popup
    protected lateinit var dialogs: Dialogs
    private var toolbar: Toolbar? = null
    var isActive = false

    private var backBtn: ImageView? = null
    private var plusLeftBtn: ImageView? = null
    private var plusRightBtn: FrameLayout? = null
    private var plusRightBtnImage: ImageView? = null
    private var plusRightBtnNotice: View? = null
    private var qrScanBtn: ImageView? = null
    private var infoBtn: ImageView? = null
    protected var closeBtn: ImageView? = null
    protected var deleteBtn: ImageView? = null
    private var settingsBtn: ImageView? = null
    protected var searchView: SearchView? = null

    private var toastLayoutTopError: ViewGroup? = null

    companion object {
        const val POP_UNTIL_ACTIVITY = "POP_UNTIL_ACTIVITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (layout != null) {
            setContentView(layout)
        }

        toastLayoutTopError = findViewById(R.id.toastLayoutTopError)
        toolbar = findViewById(R.id.toolbar)
        titleView = toolbar?.findViewById(R.id.toolbar_title)
        searchView = toolbar?.findViewById(R.id.search_tokens_view)
        setSupportActionBar(toolbar)

        backBtn = toolbar?.findViewById(R.id.toolbar_back_btn)
        plusLeftBtn = toolbar?.findViewById(R.id.toolbar_plus_btn)
        plusRightBtn = toolbar?.findViewById(R.id.toolbar_plus_btn_add_contact)
        plusRightBtnImage = toolbar?.findViewById(R.id.toolbar_plus_btn_add_contact_image)
        plusRightBtnNotice = toolbar?.findViewById(R.id.toolbar_plus_btn_add_contact_notice)
        qrScanBtn = toolbar?.findViewById(R.id.toolbar_qr_btn)
        infoBtn = toolbar?.findViewById(R.id.toolbar_info_btn)
        closeBtn = toolbar?.findViewById(R.id.toolbar_close_btn)
        deleteBtn = toolbar?.findViewById(R.id.toolbar_delete_btn)
        settingsBtn = toolbar?.findViewById(R.id.toolbar_settings_btn)

        setupActionBar(this, titleId)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }

        popup = Popup()
        dialogs = Dialogs()

        App.appCore.session.isLoggedIn.observe(this) { loggedIn ->
            if (App.appCore.setup.isAuthSetupCompleted) {
                if (loggedIn) {
                    loggedIn()
                } else {
                    loggedOut()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActive = true
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    protected open fun showError(string: String) {
        toastLayoutTopError?.let {
            popup.showSnackbarError(it, string)
        }
    }

    open fun showError(stringRes: Int) {
        toastLayoutTopError?.let {
            popup.showSnackbar(it, stringRes)
        }
    }

    open fun loggedOut() {
        val intent = Intent(this, AuthLoginActivity::class.java)
        startActivity(intent)
    }

    open fun loggedIn() {
        // do nothing
    }

    private fun setupActionBar(activity: AppCompatActivity, titleId: Int?) {
        val actionbar = activity.supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(true)

        if (titleId != null) {
            actionbar.setTitle(titleId)
            titleView?.setText(titleId)
        }
    }

    fun setActionBarTitle(titleId: Int) {
        // supportActionBar?.setTitle(titleId)
        titleView?.setText(titleId)
    }

    fun setActionBarTitle(title: String) {
        // supportActionBar?.title = title
        titleView?.text = title
    }

    fun hideActionBarTitle(isVisible: Boolean) {
        titleView?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun setActionBarTitle(title: String, subtitle: String?) {
        // supportActionBar?.title = title
        titleView?.text = title
        titleView?.setSingleLine()
    }

    fun hideAddContact(isVisible: Boolean, listener: View.OnClickListener? = null) {
        plusRightBtn?.isVisible = isVisible
        plusRightBtn?.setOnClickListener(listener)
    }

    fun hideActionBarBack(isVisible: Boolean) {
        backBtn?.isVisible = isVisible
        backBtn?.setOnClickListener {
            finish()
        }
    }

    fun hideActionBarBack(isVisible: Boolean, listener: View.OnClickListener? = null) {
        backBtn?.isVisible = isVisible
        backBtn?.setOnClickListener(listener)
    }

    fun hideActionBarDelete(isVisible: Boolean, listener: View.OnClickListener? = null) {
        deleteBtn?.isVisible = isVisible
        deleteBtn?.setOnClickListener(listener)
    }

    fun hideQrScan(isVisible: Boolean, listener: View.OnClickListener? = null) {
        qrScanBtn?.isVisible = isVisible
        qrScanBtn?.setOnClickListener(listener)
    }

    fun hideQrScan(isVisible: Boolean) {
        qrScanBtn?.isVisible = isVisible
        qrScanBtn?.setOnClickListener {
            startQrScanner()
        }
    }

    fun hideInfo(isVisible: Boolean, listener: View.OnClickListener? = null) {
        infoBtn?.isVisible = isVisible
        infoBtn?.setOnClickListener(listener)
    }

    fun hideRightPlus(
        isVisible: Boolean,
        isDisabled: Boolean = false,
        hasNotice: Boolean = false,
        listener: View.OnClickListener? = null
    ) {
        plusRightBtn?.isVisible = isVisible
        plusRightBtn?.setOnClickListener(listener)
        plusRightBtnImage?.alpha =
            if (isDisabled)
                0.7f
            else
                1f
        plusRightBtnNotice?.isVisible = hasNotice
    }

    fun hideLeftPlus(isVisible: Boolean, listener: View.OnClickListener? = null) {
        plusLeftBtn?.isVisible = isVisible
        plusLeftBtn?.setOnClickListener(listener)
    }

    fun hideSettings(isVisible: Boolean, listener: View.OnClickListener? = null) {
        settingsBtn?.isVisible = isVisible
        settingsBtn?.setOnClickListener(listener)
    }
//    fun hideClose(isVisible: Boolean) {
//        closeBtn?.visibility = if (isVisible) View.VISIBLE else View.GONE
//    }
    // authentication region

    private val scanQrResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.extras
                val qrData = data?.let(ScanQRActivity.Companion::getScannedQrContent)
                val isAddContact = data?.let(ScanQRActivity.Companion::isAddContact)
                if (qrData?.startsWith(Constants.QRPrefix.AIR_DROP) == true) { // airdrop://stage.spaceseven.cloud/api/v2/airdrop/register-wallet/3446112874593
                    Intent(applicationContext, AirdropActivity::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        it.putExtra(EXTRA_AIR_DROP_PAYLOAD, qrData)
                        startActivity(it)
                    }
                } else {
                    Intent(applicationContext, ConnectActivity::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        it.putExtra(EXTRA_CONNECT_URL, qrData)
                        it.putExtra(EXTRA_ADD_CONTACT, isAddContact)
                        startActivity(it)
                    }
                }
            }
        }

    private fun startQrScanner() {
        val intent = Intent(applicationContext, ScanQRActivity::class.java)
        intent.putExtra(EXTRA_QR_CONNECT, true)
        scanQrResult.launch(intent)
    }

    interface AuthenticationCallback {
        fun getCipherForBiometrics(): Cipher?
        fun onCorrectPassword(password: String)
        fun onCipher(cipher: Cipher)
        fun onCancelled()
    }

    protected fun showAuthentication(
        text: String?,
        shouldUseBiometrics: Boolean,
        usePasscode: Boolean,
        callback: AuthenticationCallback
    ) {
        if (shouldUseBiometrics) {
            showBiometrics(text, usePasscode, callback)
        } else {
            showPasswordDialog(text, callback)
        }
    }

    fun showAuthentication(text: String = authenticateText(), callback: AuthenticationCallback) {
        val useBiometrics = App.appCore.auth.isBiometricsUsed()
        val usePasscode = App.appCore.auth.isPasscodeUsed()
        if (useBiometrics) {
            showBiometrics(text, usePasscode, callback)
        } else {
            showPasswordDialog(text, callback)
        }
    }

    private fun showPasswordDialog(text: String?, callback: AuthenticationCallback) {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.isCancelable = false
        if (text != null) {
            val bundle = Bundle()
            bundle.putString(AuthenticationDialogFragment.EXTRA_ALTERNATIVE_TEXT, text)
            dialogFragment.arguments = bundle
        }
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                callback.onCorrectPassword(password)
            }

            override fun onCancelled() {
                callback.onCancelled()
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    private fun showBiometrics(
        text: String?,
        usePasscode: Boolean,
        callback: AuthenticationCallback
    ) {
        var biometricPrompt = createBiometricPrompt(text, callback)

        val promptInfo = createPromptInfo(text, usePasscode)
        val cipher = callback.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createBiometricPrompt(
        text: String?,
        callback: AuthenticationCallback
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onNegativeButtonClicked() {
                showPasswordDialog(text, callback)
            }

            override fun onAuthenticationSucceeded(cipher: Cipher) {
                callback.onCipher(cipher)
            }

            override fun onUserCancelled() {
                super.onUserCancelled()
                callback.onCancelled()
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    fun createPromptInfo(description: String?, usePasscode: Boolean): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setDescription(description)
            .setConfirmationRequired(true)
            .setNegativeButtonText(getString(if (usePasscode) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
    }

    fun authenticateText(): String {
        val useBiometrics = App.appCore.auth.isBiometricsUsed()
        val usePasscode = App.appCore.auth.isPasscodeUsed()
        return when {
            useBiometrics -> getString(R.string.auth_login_biometrics_dialog_subtitle)
            usePasscode -> getString(R.string.auth_login_biometrics_dialog_cancel_passcode)
            else -> getString(R.string.auth_login_biometrics_dialog_cancel_password)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        App.appCore.session.resetLogoutTimeout()
        return super.dispatchTouchEvent(event)
    }

    // end authentication region

    fun finishUntilClass(
        canonicalClassName: String?,
        thenStart: String? = null,
        withKey: String? = null,
        withData: Serializable? = null
    ) {
        canonicalClassName?.let {
            val intent = Intent()
            intent.putExtra(POP_UNTIL_ACTIVITY, it)
            thenStart?.let {
                intent.putExtra("THEN_START", thenStart)
                if (withKey != null && withData != null) {
                    intent.putExtra("WITH_KEY", withKey)
                    intent.putExtra("WITH_DATA", withData)
                }
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    // Upon returning, we check the result and pop if needed
    private val getResultGeneric =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(POP_UNTIL_ACTIVITY)?.let { className ->
                    if (this.javaClass.asSubclass(this.javaClass).canonicalName != className) {
                        finishUntilClass(
                            className,
                            it.data?.getStringExtra("THEN_START"),
                            it.data?.getStringExtra("WITH_KEY"),
                            it.data?.getSerializableExtra("WITH_DATA")
                        )
                    } else {
                        it.data?.getStringExtra("THEN_START")?.let { thenStart ->
                            val intent = Intent(this, Class.forName(thenStart))
                            if (it.data?.getStringExtra("WITH_KEY") != null && it.data?.getSerializableExtra(
                                    "WITH_DATA"
                                ) != null
                            ) {
                                intent.putExtra(
                                    it.data?.getStringExtra("WITH_KEY"),
                                    it.data?.getSerializableExtra("WITH_DATA")
                                )
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        }

    fun startActivityForResultAndHistoryCheck(intent: Intent) {
        getResultGeneric.launch(intent)
    }

    protected fun openFolderPicker(activityResult: ActivityResultLauncher<Intent>) {
        val intent: Intent?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
            intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
            val startDir = "Documents"
            var uriRoot = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI")
            var scheme = uriRoot.toString()
            scheme = scheme.replace("/root/", "/document/")
            scheme += "%3A$startDir"
            uriRoot = Uri.parse(scheme)
            intent.putExtra("android.provider.extra.INITIAL_URI", uriRoot)
        } else {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        intent.apply { flags = Intent.FLAG_GRANT_READ_URI_PERMISSION }
        activityResult.launch(intent)
    }

    fun showUnlockFeatureDialog() {
        UnlockFeatureDialog().showSingle(
            supportFragmentManager,
            UnlockFeatureDialog.TAG
        )
    }
}
