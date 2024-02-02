package com.concordium.wallet.uicore.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.DialogAuthenticationContainerBinding
import com.concordium.wallet.uicore.afterTextChanged

class AuthenticationDialogFragment : AppCompatDialogFragment(),
    TextView.OnEditorActionListener {

    private lateinit var binding: DialogAuthenticationContainerBinding

    companion object {
        const val AUTH_DIALOG_TAG = "AUTH_DIALOG_TAG"
        const val EXTRA_ALTERNATIVE_TEXT = "EXTRA_ALTERNATIVE_TEXT"
    }

    private var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(STYLE_NORMAL, R.style.AlertDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.setTitle(getString(R.string.auth_dialog_password_title))
        binding = DialogAuthenticationContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val alternativeString = arguments?.getString(EXTRA_ALTERNATIVE_TEXT)
        if (alternativeString != null) {
            binding.authenticationContent.passwordDescription.text = alternativeString
        } else {
            binding.authenticationContent.passwordDescription.setText(
                if (App.appCore.getCurrentAuthenticationManager()
                        .usePasscode()
                ) R.string.auth_dialog_passcode_description else R.string.auth_dialog_password_description
            )
        }

        binding.authenticationContent.passwordEdittext.setHint(
            if (App.appCore.getCurrentAuthenticationManager()
                    .usePasscode()
            ) R.string.auth_dialog_passcode else R.string.auth_dialog_password
        )
        if (App.appCore.getCurrentAuthenticationManager().usePasscode()) {
            binding.authenticationContent.passwordEdittext.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        binding.authenticationContent.passwordEdittext.setOnEditorActionListener(this)
        binding.authenticationContent.passwordEdittext.afterTextChanged {
            binding.authenticationContent.passwordError.isVisible = false
            binding.authenticationContent.passwordError.text = ""
        }
        binding.secondDialogButton.setOnClickListener {
            verifyPassword()
        }
        binding.cancelButton.setOnClickListener {
            callback?.onCancelled()
            dismiss()
        }

        binding.authenticationContent.passwordEdittext.requestFocus()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // The correct input mode needs to be set
            // for the keyboard to be shown automatically.
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private fun verifyPassword() {
        val passwordIsValid = App.appCore.getCurrentAuthenticationManager()
            .checkPassword(binding.authenticationContent.passwordEdittext.text.toString())
        if (passwordIsValid) {
            callback?.onCorrectPassword(binding.authenticationContent.passwordEdittext.text.toString())
            binding.authenticationContent.passwordEdittext.setText("")
            dismiss()
        } else {
            binding.authenticationContent.passwordEdittext.setText("")
            binding.authenticationContent.passwordError.isVisible = true
            binding.authenticationContent.passwordError.setText(
                if (App.appCore.getCurrentAuthenticationManager().usePasscode())
                    R.string.auth_login_passcode_error
                else
                    R.string.auth_login_password_error
            )
        }
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword()
            true
        } else false
    }

    interface Callback {
        fun onCorrectPassword(password: String)
        fun onCancelled()
    }
}
