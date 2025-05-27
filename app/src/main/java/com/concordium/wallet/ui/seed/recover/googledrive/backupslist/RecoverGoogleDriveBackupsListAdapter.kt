package com.concordium.wallet.ui.seed.recover.googledrive.backupslist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.databinding.ListItemGoogleDriveBackupBinding
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate
import com.google.api.services.drive.model.File

class RecoverGoogleDriveBackupsListAdapter :
    RecyclerView.Adapter<RecoverGoogleDriveBackupsListAdapter.ViewHolder>() {
    private val backupsList: MutableList<File> = mutableListOf()
    private var backupClickListener: BackupClickListener? = null

    inner class ViewHolder(val binding: ListItemGoogleDriveBackupBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface BackupClickListener {
        fun onBackupClick(file: File)
    }

    fun setBackupClickListener(backupClickListener: BackupClickListener) {
        this.backupClickListener = backupClickListener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFiles(files: List<File>) {
        backupsList.clear()
        backupsList.addAll(files)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemGoogleDriveBackupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = backupsList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = backupsList[position]
        holder.binding.apply {
            backupTitle.text = file.name
            backupTime.text = file.createdTime?.toString()
                ?.toDate()
                ?.formatTo("dd MMM yyyy HH:mm:ss") ?: ""

            root.setOnClickListener {
                backupClickListener?.onBackupClick(file)
            }
        }
    }
}