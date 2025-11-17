package com.example.wellsync.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wellsync.adapters.HabitsAdapter
import com.example.wellsync.databinding.FragmentHabitsBinding
import com.example.wellsync.models.Habit
import com.example.wellsync.utils.DataManager

class HabitsFragment : Fragment(), HabitsAdapter.OnHabitClickListener {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var habitsAdapter: HabitsAdapter
    private var currentUser: String? = null
    private val habits = mutableListOf<Habit>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        currentUser = dataManager.getCurrentUser()

        setupRecyclerView()
        setupUI()
        loadHabits()
    }

    private fun setupRecyclerView() {
        habitsAdapter = HabitsAdapter(habits, this)
        binding.recyclerViewHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitsAdapter
        }
    }

    private fun setupUI() {
        binding.apply {
            fabAddHabit.setOnClickListener {
                showAddHabitDialog()
            }

            btnResetHabits.setOnClickListener {
                resetDailyHabits()
            }
        }
    }

    private fun loadHabits() {
        currentUser?.let { user ->
            val userHabits = dataManager.getHabits(user)
            habits.clear()
            habits.addAll(userHabits)
            habitsAdapter.notifyDataSetChanged()
            updateProgressDisplay()
        }
    }

    private fun showAddHabitDialog() {
        if (habits.size >= 5) {
            Toast.makeText(requireContext(), "Maximum 5 habits allowed 😔", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(requireContext()).apply {
            hint = "Enter habit name (e.g., 🧘 Stretch, 📖 Read, 💧 Drink Water)"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Habit ✨")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val habitName = editText.text.toString().trim()
                if (habitName.isNotEmpty()) {
                    addHabit(habitName)
                } else {
                    Toast.makeText(requireContext(), "Please enter a habit name 😊", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addHabit(name: String) {
        val newHabit = Habit(name)
        habits.add(newHabit)
        saveHabits()
        habitsAdapter.notifyItemInserted(habits.size - 1)
        updateProgressDisplay()
        Toast.makeText(requireContext(), "Habit added successfully ✅", Toast.LENGTH_SHORT).show()
    }

    private fun saveHabits() {
        currentUser?.let { user ->
            dataManager.saveHabits(user, habits)
        }
    }

    private fun resetDailyHabits() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Daily Progress 🔄")
            .setMessage("This will reset all habit completion status for today. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                habits.forEach { it.done = false }
                saveHabits()
                habitsAdapter.notifyDataSetChanged()
                updateProgressDisplay()
                Toast.makeText(requireContext(), "Daily habits reset 🔄", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProgressDisplay() {
        val completedCount = habits.count { it.done }
        val totalCount = habits.size

        binding.apply {
            tvProgressText.text = "Progress: $completedCount / $totalCount completed"
            progressBarDaily.max = if (totalCount > 0) totalCount else 1
            progressBarDaily.progress = completedCount

            val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
            tvProgressPercentage.text = "$percentage%"
        }
    }

    override fun onHabitToggle(position: Int) {
        habits[position].done = !habits[position].done
        saveHabits()
        habitsAdapter.notifyItemChanged(position)
        updateProgressDisplay()
    }

    override fun onHabitEdit(position: Int) {
        val habit = habits[position]
        val editText = EditText(requireContext()).apply {
            setText(habit.name)
            hint = "Enter habit name"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Habit ✏️")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    habits[position] = habit.copy(name = newName)
                    saveHabits()
                    habitsAdapter.notifyItemChanged(position)
                    Toast.makeText(requireContext(), "Habit updated ✏️", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onHabitDelete(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit 🗑️")
            .setMessage("Are you sure you want to delete this habit?")
            .setPositiveButton("Delete") { _, _ ->
                habits.removeAt(position)
                saveHabits()
                habitsAdapter.notifyItemRemoved(position)
                habitsAdapter.notifyItemRangeChanged(position, habits.size)
                updateProgressDisplay()
                Toast.makeText(requireContext(), "Habit deleted 🗑️", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
