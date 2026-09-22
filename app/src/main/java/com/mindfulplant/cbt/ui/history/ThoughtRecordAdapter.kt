package com.mindfulplant.cbt.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.databinding.ItemThoughtRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThoughtRecordAdapter : ListAdapter<ThoughtRecordEntity, ThoughtRecordAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val binding = ItemThoughtRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemThoughtRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: ThoughtRecordEntity) {
            binding.dateText.text = dateFormat.format(Date(record.createdAt))
            binding.situationText.text = record.situation
            binding.distortionChip.text = record.distortionType
            binding.moodBeforeText.text = "Before 😐 ${record.moodBefore}/5"

            val context = binding.root.context
            if (record.moodAfter >= record.moodBefore) {
                binding.moodAfterText.setTextColor(ContextCompat.getColor(context, R.color.accent_success))
            } else {
                binding.moodAfterText.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
            binding.moodAfterText.text = "After 🙂 ${record.moodAfter}/5"

            if (record.syncStatus == "pending") {
                binding.syncStatusText.setBackgroundResource(R.drawable.bg_chip_amber_soft)
                binding.syncStatusText.setTextColor(ContextCompat.getColor(context, R.color.accent_amber))
                binding.syncStatusText.text = "⏳ pending"
            } else {
                binding.syncStatusText.setBackgroundResource(R.drawable.bg_chip_success_soft)
                binding.syncStatusText.setTextColor(ContextCompat.getColor(context, R.color.accent_success))
                binding.syncStatusText.text = "✓ synced"
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ThoughtRecordEntity>() {
            override fun areItemsTheSame(old: ThoughtRecordEntity, new: ThoughtRecordEntity) =
                old.recordId == new.recordId

            override fun areContentsTheSame(old: ThoughtRecordEntity, new: ThoughtRecordEntity) =
                old == new
        }
    }
}
