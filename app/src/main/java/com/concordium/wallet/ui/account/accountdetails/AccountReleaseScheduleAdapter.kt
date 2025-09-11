package com.concordium.wallet.ui.account.accountdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Schedule
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ListItemScheduledReleaseBinding
import com.concordium.wallet.databinding.ScheduledReleaseTransactionHashButtonBinding
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.DateTimeUtil
import java.util.Date

class AccountReleaseScheduleAdapter(
    private val clipboardManager: ClipboardManager,
)
    : RecyclerView.Adapter<AccountReleaseScheduleAdapter.ViewHolder>() {

    private var data: List<Schedule> = emptyList()

    @Suppress("NotifyDataSetChanged")
    fun setData(data: List<Schedule>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ListItemScheduledReleaseBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val binding = holder.binding

        binding.amountTextView.text = binding.amountTextView.context.getString(
            R.string.amount,
            CurrencyUtil.formatGTU(item.amount)
        )

        val dateTime = Date(item.timestamp)
        binding.dateTextView.text = binding.dateTextView.context.getString(
            R.string.account_release_schedule_time_at_date,
            DateTimeUtil.formatDateAsLocalShort(dateTime),
            DateTimeUtil.formatTimeAsLocal(dateTime)
        )

        binding.transactionsLayout.removeAllViews()
        item.transactions.forEach { transactionHash ->

            val transactionHashButton = ScheduledReleaseTransactionHashButtonBinding.inflate(
                LayoutInflater.from(binding.transactionsLayout.context),
                binding.transactionsLayout,
                false
            )

            transactionHashButton.root.text = transactionHash.take(8)
            transactionHashButton.root.setOnClickListener {
                val clipData = ClipData.newPlainText(
                    transactionHashButton.root.context.getString(
                        R.string.account_release_schedule_copy_title
                    ),
                    transactionHash
                )
                clipboardManager.setPrimaryClip(clipData)
                transactionHashButton.root.context.showCustomToast(
                    title = transactionHashButton.root.context.getString(
                        R.string.account_release_schedule_copied
                    ),
                )
            }

            binding.transactionsLayout.addView(transactionHashButton.root)
        }
    }

    class ViewHolder(
        val binding: ListItemScheduledReleaseBinding,
    ) : RecyclerView.ViewHolder(binding.root)
}
