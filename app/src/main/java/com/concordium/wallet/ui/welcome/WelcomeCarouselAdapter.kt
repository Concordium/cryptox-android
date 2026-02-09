package com.concordium.wallet.ui.welcome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ItemWelcomeCarouselPageBinding

class WelcomeCarouselAdapter : RecyclerView.Adapter<WelcomeCarouselAdapter.ViewHolder>() {

    private val pages = listOf(
        Page(
            imageRes = R.drawable.carousel_3,
            titleRes = R.string.welcome_your_data_title,
            descriptionRes = R.string.welcome_your_data_description,
        ),
        Page(
            imageRes = R.drawable.carousel_4,
            titleRes = R.string.welcome_speed_title,
            descriptionRes = R.string.welcome_speed_description,
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

            Glide.with(imageView.context)
                .load(page.imageRes)
                .fitCenter()
                .into(imageView)
        }
    }

    class ViewHolder(
        val binding: ItemWelcomeCarouselPageBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private class Page(
        @param:DrawableRes
        val imageRes: Int,
        @param:StringRes
        val titleRes: Int,
        @param:StringRes
        val descriptionRes: Int,
    )
}
