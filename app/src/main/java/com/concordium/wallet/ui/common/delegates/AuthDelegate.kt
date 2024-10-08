package com.concordium.wallet.ui.common.delegates

import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

typealias OnAuthenticated = (password: String) -> Unit
typealias OnCanceled = () -> Unit

interface AuthDelegate {
    fun showAuthentication(
        activity: BaseActivity,
        onCanceled: OnCanceled = {},
        onAuthenticated: OnAuthenticated,
    )
}

class AuthDelegateImpl : AuthDelegate {
    override fun showAuthentication(
        activity: BaseActivity,
        onCanceled: OnCanceled,
        onAuthenticated: OnAuthenticated,
    ) {
        activity.showAuthentication(callback = object : BaseActivity.AuthenticationCallback {
            override fun getCipherForBiometrics(): Cipher? =
                App.appCore.getCurrentAuthenticationManager()
                    .getBiometricsCipherForDecryption()

            override fun onCorrectPassword(password: String) =
                onAuthenticated(password)

            override fun onCipher(cipher: Cipher) {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    val password = App.appCore.getCurrentAuthenticationManager()
                        .decryptPasswordWithBiometricsCipher(cipher)
                    if (password != null) {
                        withContext(Dispatchers.Main) {
                            onAuthenticated(password)
                        }
                    }
                }
            }

            override fun onCancelled() =
                onCanceled()
        })
    }
}
