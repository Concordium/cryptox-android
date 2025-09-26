package com.concordium.wallet.ui.welcome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ItemWelcomeCarouselPageBinding

class WelcomeCarouselAdapter : RecyclerView.Adapter<WelcomeCarouselAdapter.ViewHolder>() {

    private val pages = listOf(
        Page(
            titleRes = R.string.welcome_verified_people_title,
            descriptionRes = R.string.welcome_verified_people_description,
        ),
        Page(
            titleRes = R.string.welcome_your_data_title,
            descriptionRes = R.string.welcome_your_data_description,
        ),
        Page(
            titleRes = R.string.welcome_verified_and_private_title,
            descriptionRes = R.string.welcome_verified_and_private_description,
        ),
        Page(
            titleRes = R.string.welcome_trust_title,
            descriptionRes = R.string.welcome_trust_description,
        ),
    )

    override fun getItemCount(): Int =
        pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemWelcomeCarouselPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]

        with(holder.binding) {
            titleTextView.text = root.context.getString(page.titleRes)
            descriptionTextView.text = root.context.getString(page.descriptionRes)
        }
    }

    class ViewHolder(
        val binding: ItemWelcomeCarouselPageBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private class Page(
        @StringRes
        val titleRes: Int,
        @StringRes
        val descriptionRes: Int,
    )
}
