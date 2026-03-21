package com.spendwise.app.ui.expense

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.databinding.ItemExpenseHistoryBinding
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.hide
import com.spendwise.app.utils.show
import com.spendwise.app.utils.toFormattedDate

class ExpenseHistoryAdapter(
    private val currency: String
) : ListAdapter<Expense, ExpenseHistoryAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    var onItemClick: ((Expense) -> Unit)? = null

    private val categoryToEmoji = mapOf(
        "Food" to "🍔", "Transport" to "🚗", "Fun" to "🎉", 
        "Health" to "💊", "Bills" to "⚡", "Shopping" to "🛍️", "Other" to "💰"
    )

    private val categoryToColor = mapOf(
        "Food" to "#FFF3E0", "Transport" to "#E3F2FD", "Fun" to "#F3E5F5", 
        "Health" to "#E8F5E9", "Bills" to "#FFF8E1", "Shopping" to "#FCE4EC", "Other" to "#EFEBE9"
    )

    fun getExpenseAt(position: Int): Expense = getItem(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(expense: Expense) {
            val emoji = categoryToEmoji[expense.category] ?: "💰"
            val colorHex = categoryToColor[expense.category] ?: "#EFEBE9"
            val parsedColor = Color.parseColor(colorHex)

            binding.tvHistoryTitle.text = expense.title
            binding.tvHistoryDate.text = expense.date.toFormattedDate()
            binding.tvHistoryAmount.text = "-${CurrencyFormatter.format(expense.amount, currency)}"
            binding.tvHistoryCategoryEmoji.text = emoji
            binding.tvHistoryCategoryName.text = expense.category

            if (expense.note.isNullOrBlank()) {
                binding.tvHistoryNote.hide()
            } else {
                binding.tvHistoryNote.text = expense.note
                binding.tvHistoryNote.show()
            }

            // Set dynamic background color to the circle view
            (binding.bgCategoryColor.background as? GradientDrawable)?.setColor(parsedColor)
            (binding.tvHistoryCategoryName.background as? GradientDrawable)?.setColor(parsedColor)
        }
    }
}

class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
    override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem == newItem
}
