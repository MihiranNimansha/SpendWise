package com.spendwise.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.databinding.ItemRecentExpenseBinding
import com.spendwise.app.utils.CategoryStyles
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.toFormattedDate

class RecentExpenseAdapter(
    private val currency: String,
    private val onItemClick: (Expense) -> Unit
) : ListAdapter<Expense, RecentExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemRecentExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(private val binding: ItemRecentExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(adapterPosition))
                }
            }
        }

        fun bind(expense: Expense) {
            binding.tvExpenseTitle.text = expense.title
            binding.tvExpenseDate.text = expense.date.toFormattedDate()
            binding.tvExpenseAmount.text = "-${CurrencyFormatter.format(expense.amount, currency)}"
            
            binding.tvCategoryEmoji.text = CategoryStyles.of(expense.category).emoji
        }
    }
}

class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
    override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
        return oldItem == newItem
    }
}
