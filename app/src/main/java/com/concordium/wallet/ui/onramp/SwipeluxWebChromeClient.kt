package com.concordium.wallet.ui.onramp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import com.concordium.wallet.App
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SwipeluxWebChromeClient(
    private val filePickerLauncher: ActivityResultLauncher<Intent>
): WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val widgetConfig = WidgetConfig(
        apiKey = "6515da6d-a065-4676-a214-c83e5b18f5f3",
        colors = Colors(
            main = "#48A2AE",
            background = "#182022",
            processing = "#FFA400",
            warning = "#ED0A34",
            success = "#58CB4E",
            link = "F24F21"
        )
    )

    private val gson = App.appCore.gson
    private val jsonString = gson.toJson(widgetConfig)
    val specificSettings: String? = URLEncoder.encode(jsonString, StandardCharsets.UTF_8.toString())

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback = filePathCallback

        val contentIntent = fileChooserParams?.createIntent()
        try {
            filePickerLauncher.launch(contentIntent)
        } catch (e: Exception) {
            this.filePathCallback = null
            return false
        }
        return true
    }

    fun handleFileChooserResult(resultCode: Int, data: Intent?) {
        if (filePathCallback == null) return

        if (resultCode == Activity.RESULT_OK) {
            val results: Array<Uri>? = data?.data?.let { arrayOf(it) }
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    companion object {
        const val BASE_URL = "https://track.swipelux.com"
    }
}