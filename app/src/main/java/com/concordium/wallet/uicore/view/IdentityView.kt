package com.concordium.wallet.uicore.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ViewIdentityBinding
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.ImageUtil

class IdentityView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val binding: ViewIdentityBinding

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        clipToPadding = false
        setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                context.resources.displayMetrics,
            ).toInt()
        )
        background = ContextCompat.getDrawable(context, R.drawable.ccx_card_dark_20_background)
        LayoutInflater.from(context).inflate(R.layout.view_identity, this, true)
        binding = ViewIdentityBinding.bind(this)
    }

    private var onItemClickListener: OnItemClickListener? = null
    private var onChangeNameClickListener: OnChangeNameClickListener? = null

    fun enableChangeNameOption(identity: Identity) {
        binding.nameTextview.compoundDrawablePadding = 20
        val drawable = AppCompatResources.getDrawable(context, R.drawable.cryptox_ico_edit)
        binding.nameTextview.setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
        binding.nameTextview.setOnClickListener {
            onChangeNameClickListener?.onChangeNameClicked(identity)
        }
    }

    fun setIdentityData(identity: Identity) {
        if (!TextUtils.isEmpty(identity.identityProvider.metadata.icon)) {
            val image = ImageUtil.getImageBitmap(identity.identityProvider.metadata.icon)
            binding.logoImageview.setImageBitmap(image)
        }

        binding.nameTextview.text = identity.name

        binding.statusTextview.setText(
            when (identity.status) {
                IdentityStatus.PENDING -> R.string.view_identity_status_pending
                IdentityStatus.DONE -> R.string.view_identity_status_done
                IdentityStatus.ERROR -> R.string.view_identity_status_error
                else -> R.string.view_identity_status_unknown
            }
        )
        binding.statusTextview.setTextColor(
            ContextCompat.getColor(
                context,
                when (identity.status) {
                    IdentityStatus.DONE -> R.color.ccx_status_success
                    IdentityStatus.ERROR -> R.color.ccx_status_error
                    else -> R.color.ccx_status_warning
                }
            )
        )

        val identityObject = identity.identityObject
        if (identityObject != null && !TextUtils.isEmpty(identityObject.attributeList.validTo)) {
            val expireDate = DateTimeUtil.convertShortDate(identityObject.attributeList.validTo)
            binding.expiresTextview.text = context.getString(
                R.string.template_view_identity_expires_on,
                expireDate
            )
            binding.expiresTextview.isVisible = true
        } else {
            binding.expiresTextview.isVisible = false
        }

        setOnClickListener {
            onItemClickListener?.onItemClicked(identity)
        }
    }

    fun interface OnItemClickListener {
        fun onItemClicked(item: Identity)
    }

    fun interface OnChangeNameClickListener {
        fun onChangeNameClicked(item: Identity)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    fun setOnChangeNameClickListener(onChangeNameClickListener: OnChangeNameClickListener) {
        this.onChangeNameClickListener = onChangeNameClickListener
    }
}
