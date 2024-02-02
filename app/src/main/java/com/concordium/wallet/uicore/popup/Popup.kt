package com.concordium.wallet.uicore.popup

import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.androidadvance.topsnackbar.TSnackbar
import com.concordium.wallet.R
import com.google.android.material.snackbar.Snackbar

class Popup {

    fun showSnackbarError(view: View, stringId: Int) {
        Snackbar.make(view, stringId, Snackbar.LENGTH_LONG).setBackgroundTint(Color.RED).show()
    }

    fun showSnackbarError(view: View, string: String) {
        val snack = TSnackbar.make(view, string, TSnackbar.LENGTH_LONG)
        snack.view.setBackgroundResource(R.color.cryptox_pinky_main)
        val textView = snack.view.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text) as TextView
        textView.setTextColor(Color.WHITE)
        snack.show()
    }

    fun showSnackbar(view: View, stringId: Int) {
        Snackbar.make(view, stringId, Snackbar.LENGTH_LONG).show()
    }

    fun showSnackbar(view: View, text: String) {
        Snackbar.make(view, text, Snackbar.LENGTH_LONG).show()
    }
}
