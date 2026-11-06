package com.spendwise.app.ui.goals

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.R
import com.spendwise.app.data.model.CategoryGoalItem
import com.spendwise.app.databinding.ItemCategoryGoalBinding
import com.spendwise.app.utils.CategoryStyles
import com.spendwise.app.utils.CurrencyFormatter
import kotlin.math.roundToInt

class CategoryGoalAdapter(
    private val currency: String
) : ListAdapter<CategoryGoalItem, CategoryGoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    var onEditClick: ((CategoryGoalItem) -> Unit)? = null

    // Explicit bold tracking colors for indicators
    private val indicatorColors = mapOf(
        "green" to "#4CAF50",
        "orange" to "#FF9800",
        "red" to "#F44336"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemCategoryGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GoalViewHolder(private val binding: ItemCategoryGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnEditGoal.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(item: CategoryGoalItem, position: Int) {
            val style = CategoryStyles.of(item.category)
            val emoji = style.emoji
            val parsedBgColor = style.pastelColor

            binding.tvCategoryName.text = item.category
            binding.tvCategoryEmoji.text = emoji
            (binding.bgCategoryColor.background as? GradientDrawable)?.setColor(parsedBgColor)
            
            val stateList = ColorStateList.valueOf(parsedBgColor)
            binding.progressCategory.setIndicatorColor(stateList.defaultColor) // Fallback default

            if (!item.hasGoal || item.monthlyLimit <= 0.0) {
                binding.tvCategorySpent.text = "No limit set"
                binding.tvCategoryPercent.text = "0%"
                binding.progressCategory.progress = 0
                binding.tvCategoryPercent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorTextSecondary))
            } else {
                val spentFormatted = CurrencyFormatter.format(item.spent, currency)
                val limitFormatted = CurrencyFormatter.format(item.monthlyLimit, currency)
                binding.tvCategorySpent.text = "$spentFormatted spent of $limitFormatted limit"
                
                var percent = ((item.spent / item.monthlyLimit) * 100).roundToInt()
                if (percent < 0) percent = 0
                
                binding.tvCategoryPercent.text = "$percent%"
                
                val statusColor = when {
                    percent < 60 -> Color.parseColor(indicatorColors["green"])
                    percent in 60..90 -> Color.parseColor(indicatorColors["orange"])
                    else -> Color.parseColor(indicatorColors["red"])
                }
                
                binding.tvCategoryPercent.setTextColor(statusColor)
                binding.progressCategory.setIndicatorColor(statusColor)

                // Animate progress
                val targetProgress = if (percent > 100) 100 else percent
                ObjectAnimator.ofInt(binding.progressCategory, "progress", 0, targetProgress).apply {
                    duration = 800
                    startDelay = (position * 100).toLong()
                    start()
                }
            }
        }
    }
}

class GoalDiffCallback : DiffUtil.ItemCallback<CategoryGoalItem>() {
    override fun areItemsTheSame(oldItem: CategoryGoalItem, newItem: CategoryGoalItem): Boolean = 
        oldItem.category == newItem.category

    override fun areContentsTheSame(oldItem: CategoryGoalItem, newItem: CategoryGoalItem): Boolean = 
        oldItem == newItem
}
