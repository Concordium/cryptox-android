package com.concordium.wallet.ui.base

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.concordium.wallet.R
import com.concordium.wallet.uicore.popup.Popup

open class BaseFragment(private val titleId: Int? = null) : Fragment() {

    private var titleView: TextView? = null
    protected lateinit var popup: Popup

    private var backBtn: ImageView? = null
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        popup = Popup()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        backBtn = toolbar?.findViewById(R.id.toolbar_back_btn)
        if (toolbar != null) {
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            titleView = toolbar?.findViewById(R.id.toolbar_title)
            setupActionBar((activity as AppCompatActivity), titleId)
        }
    }

    fun hideActionBarBack(isVisible: Boolean) {
        backBtn?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        backBtn?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    fun hideActionBarBack(isVisible: Boolean, listener: View.OnClickListener? = null) {
        backBtn?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        backBtn?.setOnClickListener(listener)
    }

    private fun setupActionBar(activity: AppCompatActivity, titleId: Int?) {
        val actionbar = activity.supportActionBar ?: return
        actionbar.setDisplayHomeAsUpEnabled(true)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }

        if (titleId != null) {
            actionbar.setTitle(titleId)
            titleView?.setText(titleId)
        }
    }
}