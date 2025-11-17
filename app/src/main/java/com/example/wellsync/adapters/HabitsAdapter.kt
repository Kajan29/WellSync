package com.example.wellsync.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wellsync.databinding.ItemHabitBinding
import com.example.wellsync.models.Habit

class HabitsAdapter(
    private val habits: List<Habit>,
    private val listener: OnHabitClickListener
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    interface OnHabitClickListener {
        fun onHabitToggle(position: Int)
        fun onHabitEdit(position: Int)
        fun onHabitDelete(position: Int)
    }

    inner class HabitViewHolder(private val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: Habit, position: Int) {
            binding.apply {
                tvHabitName.text = habit.name
                checkboxHabit.isChecked = habit.done

                if (habit.done) {
                    tvHabitName.alpha = 0.7f
                    cardHabit.alpha = 0.8f
                } else {
                    tvHabitName.alpha = 1.0f
                    cardHabit.alpha = 1.0f
                }

                checkboxHabit.setOnCheckedChangeListener { _, _ ->
                    listener.onHabitToggle(position)
                }

                cardHabit.setOnClickListener {
                    listener.onHabitToggle(position)
                }

                btnEdit.setOnClickListener {
                    listener.onHabitEdit(position)
                }

                btnDelete.setOnClickListener {
                    listener.onHabitDelete(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position], position)
    }

    override fun getItemCount() = habits.size
}
