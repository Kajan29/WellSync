package com.example.wellsync.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wellsync.databinding.ItemMoodHistoryBinding
import com.example.wellsync.models.MoodEntry
import java.text.SimpleDateFormat
import java.util.*

class MoodHistoryAdapter(
    private val moodHistory: List<MoodEntry>
) : RecyclerView.Adapter<MoodHistoryAdapter.MoodHistoryViewHolder>() {

    inner class MoodHistoryViewHolder(private val binding: ItemMoodHistoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(moodEntry: MoodEntry) {
            binding.apply {
                tvMoodEmoji.text = moodEntry.emoji
                tvMoodDate.text = moodEntry.date

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvMoodTime.text = timeFormat.format(Date(moodEntry.timestamp))

                val moodDescription = when (moodEntry.emoji) {
                    "😊" -> "Happy"
                    "😐" -> "Neutral"
                    "😔" -> "Sad"
                    "😍" -> "Excited"
                    "😠" -> "Angry"
                    else -> "Unknown"
                }
                tvMoodDescription.text = moodDescription
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodHistoryViewHolder {
        val binding = ItemMoodHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MoodHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodHistoryViewHolder, position: Int) {
        holder.bind(moodHistory[position])
    }

    override fun getItemCount() = moodHistory.size
}
