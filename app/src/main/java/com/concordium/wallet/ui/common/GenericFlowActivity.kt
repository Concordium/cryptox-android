package com.concordium.wallet.ui.common

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityIntroFlowBinding
import com.concordium.wallet.ui.account.accountdetails.WebViewPageFragment
import com.concordium.wallet.ui.base.BaseActivity


abstract class GenericFlowActivity(
    titleId: Int = R.string.app_name
) : BaseActivity(R.layout.activity_intro_flow, titleId) {
    companion object {
        const val EXTRA_HIDE_BACK = "EXTRA_HIDE_BACK"
        const val EXTRA_IGNORE_BACK_PRESS = "EXTRA_IGNORE_BACK_PRESS"
    }

    private var hideBack = true
    private var ignoreBackPress = true
    protected var showProgressLine = false
    protected var progressLineTotalDots = 4
    protected var progressLineFilledDots = 0
    private lateinit var binding: ActivityIntroFlowBinding

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroFlowBinding.bind(findViewById(R.id.root_layout))

        hideBack = intent.extras?.getBoolean(EXTRA_HIDE_BACK) == true
        ignoreBackPress = intent.extras?.getBoolean(EXTRA_IGNORE_BACK_PRESS) == true

        initializeViewModel()
        initViews()
    }

    override fun onBackPressed() {
        if (ignoreBackPress) {
            // Ignore back press
        } else {
            super.onBackPressed()
        }
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
    }

    private fun initViews() {

        binding.pager.adapter = ScreenSlidePagerAdapter(this)

        binding.createIdentIntroBack.setOnClickListener {
            binding.pager.setCurrentItem(binding.pager.currentItem - 1, true)
        }
        binding.createIdentIntroNext.setOnClickListener {
            binding.pager.setCurrentItem(binding.pager.currentItem + 1, true)
        }
        binding.createIdentIntroContinue.setOnClickListener {
            gotoContinue()
        }
        binding.createIdentIntroSkip.setOnClickListener {
            gotoContinue()
        }

        updateButtons()

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtons()
            }
        })

        hideActionBarBack(hideBack)
    }

    protected fun updateViews() {
        with (binding.progressLine) {
            isVisible = showProgressLine
            setFilledDots(progressLineFilledDots)
            setTotalDots(progressLineTotalDots)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        return if (id == android.R.id.home) {
            onBackPressed()
            true
        } else super.onOptionsItemSelected(item)
    }

    abstract fun gotoContinue()

    abstract fun getMaxPages(): Int

    abstract fun getPageTitle(position: Int): Int

    abstract fun getLink(position: Int): String

    //endregion

    //region Control/UI
    //************************************************************

    private fun updateButtons() {
        if (binding.pager.currentItem == 0 && getMaxPages() == 1) {
            binding.createIdentIntroSkip.visibility = View.GONE
            binding.createIdentIntroContinue.visibility = View.VISIBLE
            binding.createIdentIntroBack.visibility = View.GONE
            binding.createIdentIntroNext.visibility = View.GONE
        } else if (binding.pager.currentItem == 0 && getMaxPages() > 1) {
            binding.createIdentIntroSkip.visibility = View.VISIBLE
            binding.createIdentIntroContinue.visibility = View.GONE
            binding.createIdentIntroBack.visibility = View.GONE
            binding.createIdentIntroNext.visibility = View.VISIBLE
        } else if (binding.pager.currentItem == 0) {
            binding.createIdentIntroSkip.visibility = View.GONE
            binding.createIdentIntroContinue.visibility = View.GONE
            binding.createIdentIntroBack.visibility = View.GONE
            binding.createIdentIntroNext.visibility = View.VISIBLE
        } else if (binding.pager.currentItem > 0 && binding.pager.currentItem < getMaxPages() - 1) {
            binding.createIdentIntroSkip.visibility = View.GONE
            binding.createIdentIntroContinue.visibility = View.GONE
            binding.createIdentIntroBack.visibility = View.VISIBLE
            binding.createIdentIntroNext.visibility = View.VISIBLE
        } else if (binding.pager.currentItem == getMaxPages() - 1) {
            binding.createIdentIntroSkip.visibility = View.GONE
            binding.createIdentIntroContinue.visibility = View.VISIBLE
            binding.createIdentIntroNext.visibility = View.GONE
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = getMaxPages()
        override fun createFragment(position: Int): Fragment =
            WebViewPageFragment.newInstance(getLink(position), getPageTitle(position))
    }

    //endregion
}
