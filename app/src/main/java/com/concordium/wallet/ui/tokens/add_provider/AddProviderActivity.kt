package com.concordium.wallet.ui.tokens.add_provider

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.core.text.color
import androidx.core.text.underline
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.tokens.provider.ProviderMeta
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddProviderActivity : BaseActivity(R.layout.activity_add_provider) {

    private val providerPrefs = App.appCore.session.walletStorage.providerPreferences

    private val websiteUrlLayout by lazy {
        findViewById<TextInputLayout>(R.id.website_url_layout)
    }

    private val websiteUrlEt by lazy {
        findViewById<TextInputEditText>(R.id.website_url_edittext)
    }

    private val providerNameEt by lazy {
        findViewById<TextInputEditText>(R.id.provider_name_edittext)
    }

    private val providerNameLayout by lazy {
        findViewById<TextInputLayout>(R.id.provider_name_layout)
    }

    private val confirmButton by lazy {
        findViewById<Button>(R.id.confirm_button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViews()
        setActionBarTitle("Import tokens")
        hideActionBarBack(isVisible = true) {
            setResult(RESULT_CANCELED)
            finish()
        }

        confirmButton.setOnClickListener {
            var website = websiteUrlEt.text.toString().trim().trim('/')
            val providerName = providerNameEt.text.toString().trim()

            var isError = false

            websiteUrlEt.setText(website)
            providerNameEt.setText(providerName)

            if (website.isEmpty()) {
                websiteUrlLayout.error = "You need to enter website URL"
                isError = true
            }

            if (website.isNotEmpty() && !website.startsWith("http://") && !website.startsWith("https://")) {
                website = "https://$website"
            }

            if (website.isNotEmpty() && website.startsWith("http://")) {
                website = website.replace("http://", "https://")
            }

            websiteUrlEt.setText(website)

            if (!Patterns.WEB_URL.matcher(website).matches() && website.isNotEmpty()) {
                websiteUrlLayout.error = "Looks like website URL is incorrect"
                isError = true
            }

            if (providerName.isEmpty()) {
                providerNameLayout.error = "You need to enter provider name"
                isError = true
            }

            if (providerName.isNotEmpty() && providerPrefs.ifProviderNameExists(providerName)) {
                providerNameLayout.error = "This name is already registered"
                isError = true
            }

            Handler(Looper.getMainLooper()).postDelayed({
                providerNameLayout.error = null
                websiteUrlLayout.error = null
            }, 2000)

            if (website.isNotEmpty() && providerName.isNotEmpty() && !isError) {
                val provider = ProviderMeta(name = providerName, website = website)
                val providers = providerPrefs.getProviders()
                val ifExist = providers.findLast { it.website == website } != null
                if (ifExist) {
                    setResult(RESULT_CANCELED)
                    // TODO Show popup
                } else {
                    providers.add(provider)
                    providerPrefs.setProviders(providers)
                    setResult(RESULT_OK)
                }
                finish()
            }
        }
    }

    private fun initializeViews() {

        providerNameEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                confirmButton.callOnClick()
            }
            return@setOnEditorActionListener true
        }

        val sb = SpannableStringBuilder("URL: ").color(Color.parseColor("#149E7E")) {
            underline {
                append("spaceseven.com")
            }
        }
        findViewById<TextView>(R.id.desc2).text = sb
    }
}
