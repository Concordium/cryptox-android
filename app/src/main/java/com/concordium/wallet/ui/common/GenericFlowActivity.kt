package com.concordium.wallet.ui.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityIntroFlowBinding
import com.concordium.wallet.ui.account.accountdetails.WebViewPageFragment
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRegistrationNoticeFragment
import com.concordium.wallet.ui.base.BaseActivity

abstract class GenericFlowActivity(
    titleId: Int = R.string.app_name
) : BaseActivity(R.layout.activity_intro_flow, titleId) {

    private lateinit var binding: ActivityIntroFlowBinding

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroFlowBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initViews() {
        binding.introFlowContainer.removeAllViews()
        if (showNotice()) {
            addFragment(BakerRegistrationNoticeFragment(), binding.introFlowContainer.id)
        }

        getTitles().forEachIndexed { index, _ ->
            addFragment(
                WebViewPageFragment.newInstance(getLink(index), getPageTitle(index)),
                binding.introFlowContainer.id
            )
        }

        with(binding.continueButton) {
            text = getButtonText()
            setOnClickListener {
                gotoContinue()
            }
            isEnabled = isButtonEnabled()
        }

        binding.scrollContainer.viewTreeObserver.addOnScrollChangedListener {
            val scrollView = binding.scrollContainer
            val view = scrollView.getChildAt(scrollView.childCount - 1)

            if (view.bottom <= (scrollView.height + scrollView.scrollY))
                binding.continueButton.isEnabled = true
        }
    }

    private fun addFragment(fragment: Fragment, containerId: Int) {
        supportFragmentManager.commit {
            add(containerId, fragment, fragment::class.java.simpleName)
        }
    }

    abstract fun gotoContinue()

    abstract fun getPageTitle(position: Int): Int

    abstract fun getLink(position: Int): String

    abstract fun getTitles(): IntArray

    abstract fun getButtonText(): String

    abstract fun isButtonEnabled(): Boolean

    open fun showNotice(): Boolean = false

    //endregion
}
